package com.bible.reference.explorer.api.Repository;

/**
 * DTO for one row of {@link VerseRepository#findReferenceGraph(String, int)}:
 * the two verse endpoints ({@code origin}/{@code peer}) of the top-ranked
 * reference edges off the requested verse, plus the edge itself
 * ({@code edge}) and one further edge one hop out ({@code nextEdge}).
 * Mirrors the shape the hand-written Cypher used to return as raw driver
 * {@code Node}/{@code Relationship} values (there: {@code o}/{@code rel}/
 * {@code p}/{@code n}).
 *
 * <p>Deliberately a record (constructor-parameter DTO binding), not an
 * interface projection: Spring Data Neo4j routes every interface-projection
 * getter on a {@code Neo4jRepository<VerseEntity, Long>} query through
 * {@code EntityAndGraphPropertyAccessingMethodInterceptor}, which resolves
 * it as a graph-property path off {@code VerseEntity} rather than as a raw
 * query-result column -- and throws
 * {@code IllegalStateException: Invalid property '...' of bean class
 * VerseEntity} since none of these column names are real entity properties.
 * A record's constructor parameters are bound by name directly against the
 * query result instead, bypassing that interceptor entirely.</p>
 */
public record VerseReferenceRow(
		Long originId,
		String originTitle,
		String originBook,
		String originChapter,
		String originVerse,
		Integer edgeRank,
		Long edgeFrom,
		Long edgeTo,
		Long peerId,
		String peerTitle,
		String peerBook,
		String peerChapter,
		String peerVerse,
		Integer nextEdgeRank,
		Long nextEdgeFrom,
		Long nextEdgeTo) {
}
