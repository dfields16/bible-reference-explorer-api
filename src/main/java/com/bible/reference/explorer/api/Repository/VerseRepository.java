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
	 * Cypher returned (columns {@code o}/{@code rel}/{@code p}/{@code n}), just
	 * mapped into real entities/projections instead of raw driver
	 * {@code Node}/{@code Relationship} objects.
	 */
	@Query("""
			CALL {
			  MATCH (v:Verse)-[rel:references]-(p:Verse)
			  WHERE v.title = $verseTitle
			  RETURN rel
			  ORDER BY rel.rank DESC
			  LIMIT $limit
			}
			OPTIONAL MATCH (o:Verse)-[rel]-(p:Verse)
			OPTIONAL MATCH (p:Verse)-[n:references]-(a:Verse)
			RETURN o AS o,
			       {rank: toInteger(rel.rank), from: id(startNode(rel)), to: id(endNode(rel))} AS rel,
			       p AS p,
			       {rank: toInteger(n.rank), from: id(startNode(n)), to: id(endNode(n))} AS n
			""")
	List<VerseReferenceRow> findReferenceGraph(@Param("verseTitle") String verseTitle, @Param("limit") int limit);
}
