package com.bible.reference.explorer.api.Controller;

import static org.neo4j.driver.Values.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.bible.reference.explorer.api.Utils.BibleVerseUtil;
import com.bible.reference.explorer.api.model.BibleApi.VerseApi;
import com.bible.reference.explorer.api.model.BibleApi.VerseApiRaw;
import com.bible.reference.explorer.api.model.BibleApi.VerseRequest;
import com.bible.reference.explorer.api.model.BibleApi.VerseResponse;
import com.bible.reference.explorer.api.model.Neo4j.CrossReferenceResult;
import com.bible.reference.explorer.api.model.Neo4j.References;
import com.bible.reference.explorer.api.model.Neo4j.Verse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
public class BibleVerseController {

	@Autowired
	protected Session session;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected BibleVerseUtil bibleVerseUtil;

	@Autowired
	protected WebClient bibleWebClient;

	@GetMapping("/getReferences/{verse}/{limit}")
	public CrossReferenceResult getMappingString(@PathVariable("verse") String verse, @PathVariable("limit") int limit) throws Exception {
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

	@GetMapping("/findShortestPath/{verse1}/{verse2}/{limit}")
	public CrossReferenceResult findShortestPath(@PathVariable("verse1") String v1, @PathVariable("verse2") String v2, @PathVariable("limit") int limit) throws Exception {
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

	@GetMapping("/verify/{verse}")
	public boolean verifyVerseApi(@PathVariable("verse") String verse) throws Exception {
		try {
			bibleVerseUtil.verifyVerse(verse, true);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@PostMapping("/verses")
	public VerseResponse getVersesFromBibleAPI(@RequestBody VerseRequest req){
		List<VerseApi> verses = Flux.fromStream(req.getVerses().stream())
				.parallel()
				.map(x-> {
					try {
						return bibleVerseUtil.verifyVerse(x, false);
					} catch (Exception e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.flatMap(x -> {
					return bibleWebClient
							.get()
							.uri(uriBuilder -> uriBuilder
									.path("bibles/{bible}/verses/{verse}")
									.queryParam("content-type",            "html")
									.queryParam("include-notes",           false)
									.queryParam("include-titles",          true)
									.queryParam("include-chapter-numbers", false)
									.queryParam("include-verse-numbers",   true)
									.queryParam("include-verse-spans",     false)
									.queryParam("use-org-id",              false)
									.build(Map.of("verse", x, "bible", req.getBibleVersion().id)))
							.accept(MediaType.APPLICATION_JSON)
							.retrieve()
							.bodyToMono(VerseApiRaw.class)
							.retry(3);
				})
				.sequential()
				.toStream()
				.map(VerseApi::new)
				.collect(Collectors.toList());

		if(!CollectionUtils.isEmpty(verses)){
			return VerseResponse.builder()
				.version(req.getBibleVersion().name())
				.copyright(verses.get(0).getCopyright())
				.verses(verses)
			.build();
		}

		return null;
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
