package com.bible.reference.explorer.api.Components;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import com.bible.reference.explorer.api.Repository.VerseReferenceRow;
import com.bible.reference.explorer.api.Repository.VerseRepository;
import com.bible.reference.explorer.api.model.Neo4j.CrossReferenceResult;
import com.bible.reference.explorer.api.model.Neo4j.References;
import com.bible.reference.explorer.api.model.Neo4j.Verse;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the graph traversals backing {@code BibleVerseController},
 * built on top of real Spring Data Neo4j / Cypher-DSL querying instead of a
 * hand-rolled driver {@code Session}/{@code Driver} or hand-written Cypher
 * strings. Owns the response-shaping into {@link CrossReferenceResult} and
 * the caching that a plain {@code Neo4jRepository} can't express by itself
 * -- this is exactly what used to live in the {@code @Component}-annotated
 * class also named {@code VerseRepository}, now split so that name can
 * belong to the actual Spring Data repository interface.
 *
 * <p>Every query here goes through {@link Neo4jClient} with a Cypher-DSL
 * {@link Statement}, not a {@code @Query}-annotated {@link VerseRepository}
 * method: {@code @Query}'s value is a compile-time-constant annotation
 * attribute, so it can only ever hold a literal Cypher string, never a
 * statement built (and thus type-checked) at runtime by the DSL.</p>
 *
 * <p>Statements are rendered with {@link Dialect#NEO4J_4}, the most
 * conservative dialect Cypher-DSL offers, deliberately -- the newer, more
 * "current" dialects emit syntax (e.g. the {@code CALL () { ... }}
 * variable-scope-clause form, requiring Neo4j 5.9+) that this app's actual
 * production server has already been confirmed to reject outright with a
 * syntax error, not just a deprecation warning. Don't raise this without
 * confirming the server version in use.</p>
 */
@Slf4j
@Service
public class VerseGraphService {

	private static final Renderer RENDERER = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_4).build());

	/**
	 * {@code allShortestPaths} requires its hop bound to be a literal in the
	 * Cypher text: Neo4j does not allow a parameter inside a variable-length
	 * relationship range ({@code *1..$n}), so it can never be a bound query
	 * parameter -- that's why {@link #findShortestPath} still builds its
	 * Cypher as a plain, hand-written text block rather than through the DSL
	 * (whose typed pattern-length API models a bounded range, not the
	 * classic {@code allShortestPaths(...)} function call this app's Neo4j
	 * version needs -- see the class-level dialect note). The caller-supplied
	 * hop count is clamped into this range before being spliced in, purely as
	 * a defensive bound on how expensive the path search can get; the verse
	 * identifiers themselves (v1/v2) are always passed as real bound
	 * parameters, never concatenated -- that's the actual injection fix.
	 */
	private static final int MIN_HOPS = 1;
	private static final int MAX_HOPS = 50;

	private static final int MAX_SHORTEST_PATHS = 10;

	@Autowired
	protected Neo4jClient neo4jClient;

	@Cacheable("graph-references")
	public CrossReferenceResult getReferences(String verse, int limit) {
		try {
			Node v = Cypher.node("Verse").named("v");
			Node p = Cypher.node("Verse").named("p");
			Relationship topEdge = v.relationshipBetween(p, "references").named("rel");

			Statement topRankedEdges = Cypher.match(topEdge)
					.where(v.property("title").isEqualTo(Cypher.parameter("verseTitle")))
					.returning(topEdge)
					.orderBy(topEdge.property("rank")).descending()
					.limit(Cypher.parameter("limit"))
					.build();

			Node origin = Cypher.node("Verse").named("o");
			Node peer = Cypher.node("Verse").named("p");
			Relationship edge = origin.relationshipBetween(peer).named("rel");

			Node further = Cypher.node("Verse").named("a");
			Relationship nextEdge = peer.relationshipBetween(further, "references").named("n");

			Statement statement = Cypher.call(topRankedEdges)
					.optionalMatch(edge)
					.optionalMatch(nextEdge)
					.returning(
							origin.internalId().as("originId"),
							origin.property("title").as("originTitle"),
							origin.property("book").as("originBook"),
							origin.property("chapter").as("originChapter"),
							origin.property("verse").as("originVerse"),
							Cypher.toInteger(edge.property("rank")).as("edgeRank"),
							Cypher.raw("id(startNode($E))", Cypher.name("rel")).as("edgeFrom"),
							Cypher.raw("id(endNode($E))", Cypher.name("rel")).as("edgeTo"),
							peer.internalId().as("peerId"),
							peer.property("title").as("peerTitle"),
							peer.property("book").as("peerBook"),
							peer.property("chapter").as("peerChapter"),
							peer.property("verse").as("peerVerse"),
							Cypher.toInteger(nextEdge.property("rank")).as("nextEdgeRank"),
							Cypher.raw("id(startNode($E))", Cypher.name("n")).as("nextEdgeFrom"),
							Cypher.raw("id(endNode($E))", Cypher.name("n")).as("nextEdgeTo"))
					.build();

			Collection<VerseReferenceRow> rows = neo4jClient.query(RENDERER.render(statement))
					.bind(verse).to("verseTitle")
					.bind(limit).to("limit")
					.fetchAs(VerseReferenceRow.class)
					.mappedBy((typeSystem, record) -> new VerseReferenceRow(
							nullableLong(record.get("originId")),
							nullableString(record.get("originTitle")),
							nullableString(record.get("originBook")),
							nullableString(record.get("originChapter")),
							nullableString(record.get("originVerse")),
							nullableInt(record.get("edgeRank")),
							nullableLong(record.get("edgeFrom")),
							nullableLong(record.get("edgeTo")),
							nullableLong(record.get("peerId")),
							nullableString(record.get("peerTitle")),
							nullableString(record.get("peerBook")),
							nullableString(record.get("peerChapter")),
							nullableString(record.get("peerVerse")),
							nullableInt(record.get("nextEdgeRank")),
							nullableLong(record.get("nextEdgeFrom")),
							nullableLong(record.get("nextEdgeTo"))))
					.all();

			Set<Verse> verses = new HashSet<>();
			Set<References> references = new HashSet<>();
			Set<String> mutualEdges = new HashSet<>();

			for (VerseReferenceRow row : rows) {
				addVerse(verses, row.originId(), row.originTitle(), row.originBook(), row.originChapter(), row.originVerse());
				addVerse(verses, row.peerId(), row.peerTitle(), row.peerBook(), row.peerChapter(), row.peerVerse());
				addEdge(references, mutualEdges, row.edgeFrom(), row.edgeTo(), row.edgeRank());
				addEdge(references, mutualEdges, row.nextEdgeFrom(), row.nextEdgeTo(), row.nextEdgeRank());
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

			// A single connected pattern -- v1/v2's constraints live inside the
			// allShortestPaths(...) match itself, not as separate comma-listed
			// MATCHes -- so Neo4j never treats them as disconnected patterns
			// requiring a cartesian product before the path constraint applies.
			String nodesCypher = """
					MATCH p = allShortestPaths((v1:Verse {title: $v1})-[:references*1..%d]-(v2:Verse {title: $v2}))
					WITH p LIMIT %d
					UNWIND range(0, size(nodes(p)) - 1) AS level
					WITH nodes(p)[level] AS n, level
					RETURN id(n) AS id, n.title AS title, n.book AS book, n.chapter AS chapter, n.verse AS verse, level AS level
					""".formatted(hopBound, MAX_SHORTEST_PATHS);

			String edgesCypher = """
					MATCH p = allShortestPaths((v1:Verse {title: $v1})-[:references*1..%d]-(v2:Verse {title: $v2}))
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

	private static void addVerse(Set<Verse> verses, Long id, String title, String book, String chapter, String verse) {
		if (id != null) {
			verses.add(Verse.builder()
					.id(id)
					.title(title)
					.book(book)
					.chapter(chapter)
					.verse(verse)
					.label(book + " " + chapter + ":" + verse)
					.build());
		}
	}

	/**
	 * Skips adding an edge if its reverse direction has already been added,
	 * so a mutual {@code references} relationship between two verses (present
	 * in the graph as both A-&gt;B and B-&gt;A) only shows up once.
	 */
	private static void addEdge(Set<References> references, Set<String> mutualEdges, Long from, Long to, Integer rank) {
		if (from == null || to == null) {
			return;
		}
		if (mutualEdges.contains(to + "->" + from)) {
			return;
		}
		mutualEdges.add(from + "->" + to);
		references.add(References.builder().from(from).to(to).rank(rank).build());
	}

	/**
	 * {@code Value.asLong()}/{@code .asString()}/etc. throw on a null value --
	 * unlike Spring Data Neo4j's own record/projection binding (which returns
	 * null automatically), a manual {@link Neo4jClient} {@code .mappedBy(...)}
	 * has to do that conversion itself. Every {@code origin}/{@code peer}/
	 * {@code edge}/{@code nextEdge} column in {@link #getReferences} can be
	 * null (they come from {@code OPTIONAL MATCH}), so every read goes
	 * through one of these.
	 */
	private static Long nullableLong(Value value) {
		return value.isNull() ? null : value.asLong();
	}

	private static Integer nullableInt(Value value) {
		return value.isNull() ? null : value.asInt();
	}

	private static String nullableString(Value value) {
		return value.isNull() ? null : value.asString();
	}

	private record PathNode(Long id, String title, String book, String chapter, String verse, Integer level) {
	}

	private record PathEdge(Long from, Long to, Integer rank) {
	}
}
