package com.bible.reference.explorer.api.Components;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import com.bible.reference.explorer.api.Repository.VerseEdge;
import com.bible.reference.explorer.api.Repository.VerseReferenceRow;
import com.bible.reference.explorer.api.Repository.VerseRepository;
import com.bible.reference.explorer.api.model.Neo4j.CrossReferenceResult;
import com.bible.reference.explorer.api.model.Neo4j.References;
import com.bible.reference.explorer.api.model.Neo4j.Verse;
import com.bible.reference.explorer.api.model.Neo4j.VerseEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the graph traversals backing {@code BibleVerseController},
 * built on top of the real {@link VerseRepository} (Spring Data Neo4j)
 * instead of a hand-rolled driver {@code Session}/{@code Driver}. Owns the
 * response-shaping into {@link CrossReferenceResult} and the caching that a
 * plain {@code Neo4jRepository} can't express by itself -- this is exactly
 * what used to live in the {@code @Component}-annotated class also named
 * {@code VerseRepository}, now split so that name can belong to the actual
 * Spring Data repository interface.
 */
@Slf4j
@Service
public class VerseGraphService {

	/**
	 * {@code allShortestPaths} requires its hop bound to be a literal in the
	 * Cypher text: Neo4j does not allow a parameter inside a variable-length
	 * relationship range ({@code *1..$n}), so it can never be a bound
	 * {@code @Query} parameter -- that's why {@link #findShortestPath} can't be
	 * expressed as a static repository {@code @Query} the way
	 * {@link VerseRepository#findReferenceGraph} is, and instead builds its
	 * Cypher text via {@link Neo4jClient}. The caller-supplied hop count is
	 * clamped into this range before being spliced in, purely as a defensive
	 * bound on how expensive the path search can get; the verse identifiers
	 * themselves (v1/v2) are always passed as real bound parameters, never
	 * concatenated -- that's the actual injection fix.
	 */
	private static final int MIN_HOPS = 1;
	private static final int MAX_HOPS = 50;

	private static final int MAX_SHORTEST_PATHS = 10;

	@Autowired
	protected VerseRepository verseRepository;

	@Autowired
	protected Neo4jClient neo4jClient;

	@Cacheable("graph-references")
	public CrossReferenceResult getReferences(String verse, int limit) {
		try {
			List<VerseReferenceRow> rows = verseRepository.findReferenceGraph(verse, limit);

			Set<Verse> verses = new HashSet<>();
			Set<References> references = new HashSet<>();

			for (VerseReferenceRow row : rows) {
				addVerse(verses, row.getO());
				addVerse(verses, row.getP());
				addEdge(references, row.getRel());
				addEdge(references, row.getN());
			}

			Map<Long, Verse> verseMap = verses.stream().collect(Collectors.toMap(Verse::getId, Function.identity()));
			references.removeIf(ref -> !verseMap.containsKey(ref.getFrom()) || !verseMap.containsKey(ref.getTo()));

			return new CrossReferenceResult(verses, references);
		} catch (Exception e) {
			log.error("Error getting references for verse={} with message={} with stackstrace={}", verse, e.getMessage(), e.getStackTrace());
			return null;
		}
	}

	@Cacheable("graph-shortest-path")
	public CrossReferenceResult findShortestPath(String v1, String v2, int maxPath) {
		try {
			int hopBound = Math.max(MIN_HOPS, Math.min(maxPath, MAX_HOPS));

			String nodesCypher = """
					MATCH (v1:Verse {title: $v1}), (v2:Verse {title: $v2})
					MATCH p = allShortestPaths((v1)-[:references*1..%d]-(v2))
					WITH p LIMIT %d
					UNWIND range(0, size(nodes(p)) - 1) AS level
					WITH nodes(p)[level] AS n, level
					RETURN id(n) AS id, n.title AS title, n.book AS book, n.chapter AS chapter, n.verse AS verse, level AS level
					""".formatted(hopBound, MAX_SHORTEST_PATHS);

			String edgesCypher = """
					MATCH (v1:Verse {title: $v1}), (v2:Verse {title: $v2})
					MATCH p = allShortestPaths((v1)-[:references*1..%d]-(v2))
					WITH p LIMIT %d
					UNWIND relationships(p) AS rel
					RETURN id(startNode(rel)) AS from, id(endNode(rel)) AS to, toInteger(rel.rank) AS rank
					""".formatted(hopBound, MAX_SHORTEST_PATHS);

			Collection<PathNode> nodeRows = neo4jClient.query(nodesCypher)
					.bind(v1).to("v1")
					.bind(v2).to("v2")
					.fetchAs(PathNode.class)
					.mappedBy((typeSystem, record) -> new PathNode(
							record.get("id").asLong(),
							record.get("title").asString(),
							record.get("book").asString(),
							record.get("chapter").asString(),
							record.get("verse").asString(),
							record.get("level").asInt()))
					.all();

			Collection<PathEdge> edgeRows = neo4jClient.query(edgesCypher)
					.bind(v1).to("v1")
					.bind(v2).to("v2")
					.fetchAs(PathEdge.class)
					.mappedBy((typeSystem, record) -> new PathEdge(
							record.get("from").asLong(),
							record.get("to").asLong(),
							record.get("rank").asInt()))
					.all();

			Set<Verse> verses = nodeRows.stream()
					.map(row -> Verse.builder()
							.id(row.id())
							.title(row.title())
							.book(row.book())
							.chapter(row.chapter())
							.verse(row.verse())
							.label(row.book() + " " + row.chapter() + ":" + row.verse())
							.level(String.valueOf(row.level()))
							.build())
					.collect(Collectors.toSet());

			Set<References> references = edgeRows.stream()
					.map(row -> References.builder().from(row.from()).to(row.to()).rank(row.rank()).build())
					.collect(Collectors.toSet());

			return new CrossReferenceResult(verses, references);
		} catch (Exception e) {
			log.error("Error getting shortest path for verse1={} verse2={} with message={} with stackstrace={}", v1, v2, e.getMessage(), e.getStackTrace());
			return null;
		}
	}

	private static void addVerse(Set<Verse> verses, VerseEntity entity) {
		if (entity != null) {
			verses.add(Verse.of(entity));
		}
	}

	private static void addEdge(Set<References> references, VerseEdge edge) {
		if (edge != null && edge.getFrom() != null && edge.getTo() != null) {
			references.add(References.builder().from(edge.getFrom()).to(edge.getTo()).rank(edge.getRank()).build());
		}
	}

	private record PathNode(Long id, String title, String book, String chapter, String verse, Integer level) {
	}

	private record PathEdge(Long from, Long to, Integer rank) {
	}
}
