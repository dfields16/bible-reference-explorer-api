package com.bible.reference.explorer.api.Controller;

import static org.neo4j.driver.Values.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.bible.reference.explorer.api.model.BibleBook;
import com.bible.reference.explorer.api.model.CrossReferenceResult;
import com.bible.reference.explorer.api.model.References;
import com.bible.reference.explorer.api.model.Verse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class BibleVerseController {

	@Autowired
	public Session session;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	Map<String, BibleBook> bibleMap;

	@GetMapping("/api/getReferences/{verse}/{limit}")
	public CrossReferenceResult getMappingString(@PathVariable("verse") String verse, @PathVariable("limit") int limit)
			throws Exception {
		try {
			String verseReference = verifyVerse(verse);
			int validLimit = verifyLimit(limit);
			log.info("Querying verse={} with limit={}", verseReference, validLimit);

			Set<Verse> verses = new HashSet<>();
			Set<References> references = new HashSet<>();

			session.run(getVerseQuery(verseReference, validLimit)).list(x -> {
				verses.add(new Verse(x.get("o").asNode()));
				verses.add(new Verse(x.get("p").asNode()));
				references.add(new References(x.get("rel").asRelationship()));
				references.add(new References(x.get("n").asRelationship()));
				return x;
			});

			Map<String, Verse> verseMap = verses.stream().collect(Collectors.toMap(x->x.getId(), Function.identity()));
			references.removeIf(ref ->{
				return !verseMap.containsKey(ref.getFrom()) || !verseMap.containsKey(ref.getTo());
			});

			return new CrossReferenceResult(verses, references);
		} catch (Exception e) {
			return null;
		}
	}

	@GetMapping("/api/verify/{verse}")
	public boolean verifyVerseApi(@PathVariable("verse") String verse) throws Exception {
		try {
			verifyVerse(verse);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private int verifyLimit(int limit) {
		if (limit <= 10) {
			return 10;
		} else if (limit >= 50) {
			return 50;
		}
		return limit;
	}

	public String verifyVerse(String bibleRef) throws Exception {
		String[] parts = bibleRef.split("\\.");
		String book = parts[0];
		Integer chapter = Integer.valueOf(parts[1]);
		Integer verse = Integer.valueOf(parts[2]);

		BibleBook bibleBook = bibleMap.get(book);
		if (0 < chapter && chapter <= bibleBook.getChapterCount()) {
			if (0 < verse && verse <= bibleBook.getVerseCountList().get(chapter - 1)) {
				return bibleBook.getDbKey() + "." + chapter + "." + verse;
			}
		}
		throw new Exception("Invalid Bible reference.");
	}

	public Query getVerseQuery(String verseId, int limit) {
		return new Query(
				"CALL{" +
						"	MATCH (v:Verse)-[rel:references]-(p:Verse) WHERE v.title = $verseTitle" +
						"	RETURN rel" +
						"	ORDER BY rel.rank DESC" +
						"	LIMIT $limit" +
						"}" +
						"OPTIONAL MATCH (o:Verse)-[rel]-(p:Verse)" +
						"OPTIONAL MATCH (p:Verse)-[n:references]-(a:Verse)" +
						"RETURN o,rel,p,n ",
				parameters("verseTitle", verseId, "limit", limit));
	}

}
