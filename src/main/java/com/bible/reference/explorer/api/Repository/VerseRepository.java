package com.bible.reference.explorer.api.Repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.bible.reference.explorer.api.model.Neo4j.VerseEntity;

/**
 * Spring Data Neo4j repository for {@link VerseEntity}, replacing the raw
 * driver {@code Session}/hand-written Cypher strings the app used to run
 * directly.
 *
 * <p>The reference-graph traversal below is expressed as a {@code @Query}
 * because it isn't a shape the derived-query mechanism can produce (a ranked
 * {@code LIMIT}ed subquery joined with two further optional hops). Every
 * user-supplied value is bound through a named parameter -- never
 * concatenated into the Cypher text.</p>
 */
public interface VerseRepository extends Neo4jRepository<VerseEntity, Long> {

	/**
	 * The top {@code limit} {@code references} edges (by rank) attached to the
	 * verse titled {@code verseTitle}, plus, for each of those edges' far
	 * endpoint, its own {@code references} edges one hop further out. This
	 * reproduces the shape the original hand-written {@code getVerseQuery}
	 * Cypher returned (columns {@code o}/{@code rel}/{@code p}/{@code n}, here
	 * named {@code origin}/{@code edge}/{@code peer}/{@code nextEdge}).
	 *
	 * <p>Every column is a flat scalar, and {@link VerseReferenceRow}'s
	 * getters are flat scalars too -- deliberately not nested projections
	 * (e.g. a {@code getOrigin()} returning some {@code VerseNode} type), for
	 * two reasons that only surface once a query actually runs against a real
	 * database: returning a verse as a real {@code Verse} node trips "More
	 * than one matching node in the record" when the same row also carries
	 * the *other* endpoint as a same-labelled node (SDN can't tell which of
	 * two same-labelled nodes in one row is the query root), and returning it
	 * as a map literal mapped through a *nested* projection interface trips
	 * "Invalid property 'x' of bean class VerseEntity" (SDN resolves a nested
	 * projection's getters as graph-property paths off the repository's
	 * entity type, not as raw columns). Flattening every field onto
	 * {@link VerseReferenceRow} directly avoids both.</p>
	 *
	 * <p>Column/getter names also avoid a two-letter prefix immediately
	 * followed by another capital (e.g. the previous {@code oId}/{@code pId})
	 * -- {@code java.beans.Introspector.decapitalize} special-cases two
	 * leading capitals as an acronym and leaves them as-is, so
	 * {@code getOId()} resolves to property {@code "OId"}, not {@code "oId"},
	 * silently breaking the column-name match.</p>
	 */
	@Query("""
			CALL () {
			  MATCH (v:Verse)-[rel:references]-(p:Verse)
			  WHERE v.title = $verseTitle
			  RETURN rel
			  ORDER BY rel.rank DESC
			  LIMIT $limit
			}
			OPTIONAL MATCH (o:Verse)-[rel]-(p:Verse)
			OPTIONAL MATCH (p:Verse)-[n:references]-(a:Verse)
			RETURN id(o) AS originId, o.title AS originTitle, o.book AS originBook, o.chapter AS originChapter, o.verse AS originVerse,
			       toInteger(rel.rank) AS edgeRank, id(startNode(rel)) AS edgeFrom, id(endNode(rel)) AS edgeTo,
			       id(p) AS peerId, p.title AS peerTitle, p.book AS peerBook, p.chapter AS peerChapter, p.verse AS peerVerse,
			       toInteger(n.rank) AS nextEdgeRank, id(startNode(n)) AS nextEdgeFrom, id(endNode(n)) AS nextEdgeTo
			""")
	List<VerseReferenceRow> findReferenceGraph(@Param("verseTitle") String verseTitle, @Param("limit") int limit);
}
