package com.bible.reference.explorer.api.Components;

import static org.neo4j.driver.Values.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.bible.reference.explorer.api.Utils.BibleVerseUtil;
import com.bible.reference.explorer.api.model.Neo4j.CrossReferenceResult;
import com.bible.reference.explorer.api.model.Neo4j.References;
import com.bible.reference.explorer.api.model.Neo4j.Verse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VerseRepository {

	@Autowired
	protected Session session;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected BibleVerseUtil bibleVerseUtil;

	@Autowired
	protected WebClient bibleWebClient;


	@Cacheable("graph-references")
	public CrossReferenceResult getReferences(String verse, int limit){
		try {
			String verseReference = bibleVerseUtil.verifyVerse(verse, true);
			int validLimit = bibleVerseUtil.verifyLimit(limit);
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

			Map<String, Verse> verseMap = verses.stream().collect(Collectors.toMap(x -> x.getId(), Function.identity()));
			references.removeIf(ref -> {
				return !verseMap.containsKey(ref.getFrom()) || !verseMap.containsKey(ref.getTo());
			});

			return new CrossReferenceResult(verses, references);
		} catch (Exception e) {
			return null;
		}
	}

	@Cacheable("graph-shortest-path")
	public CrossReferenceResult findShortestPath(String v1, String v2, int limit) {
		try {
			String verse1 = bibleVerseUtil.verifyVerse(v1, true);
			String verse2 = bibleVerseUtil.verifyVerse(v2, true);

			Set<Verse> verses = new HashSet<>();
			Set<References> references = new HashSet<>();

			session.run(shortestPathQuery(verse1, verse2, limit)).list(x -> {
				AtomicInteger level = new AtomicInteger(0);
				verses.addAll(x.get("NODES(p)").asList(node->node.asNode()).stream().map(node -> new Verse(node)).peek(n->n.setLevel(String.valueOf(level.getAndIncrement()))).collect(Collectors.toList()));
				references.addAll(x.get("RELATIONSHIPS(p)").asList(ref->ref.asRelationship()).stream().map(ref -> new References(ref)).collect(Collectors.toList()));
				return x;
			});

			return new CrossReferenceResult(verses, references);
		} catch (Exception e) {
			return null;
		}
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

	public Query shortestPathQuery(String v1, String v2, int maxPath) {
		return new Query(
			"MATCH(v1:Verse{title:'" + v1 + "'}), (v2:Verse{title:'" + v2 +"'}) , p=allshortestpaths((v1)-[:references*1.." + maxPath + "]-(v2)) RETURN NODES(p), RELATIONSHIPS(p) LIMIT 10"
		);
	}
}
