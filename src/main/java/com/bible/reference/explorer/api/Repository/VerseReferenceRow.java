package com.bible.reference.explorer.api.Repository;

/**
 * DTO for one row of the reference-graph traversal run by
 * {@code VerseGraphService#getReferences}: the two verse endpoints
 * ({@code origin}/{@code peer}) of the top-ranked reference edges off the
 * requested verse, plus the edge itself ({@code edge}) and one further edge
 * one hop out ({@code nextEdge}). Mirrors the shape the original
 * hand-written Cypher returned as raw driver {@code Node}/
 * {@code Relationship} values (there: {@code o}/{@code rel}/{@code p}/
 * {@code n}).
 *
 * <p>Every field is nullable: {@code origin}/{@code peer}/{@code edge}/
 * {@code nextEdge} each come from an {@code OPTIONAL MATCH}, and
 * {@code VerseGraphService} constructs this record itself from a
 * {@code Neo4jClient} {@code .mappedBy(...)} callback reading the query
 * result column by column, not through Spring Data Neo4j's own entity or
 * projection mapping -- so if this ever moves back to a
 * {@code @Query}-annotated {@code Neo4jRepository<VerseEntity, Long>}
 * method, keep this an immutable record (constructor-parameter DTO
 * binding), not an interface projection: SDN routes every
 * interface-projection getter on such a query through
 * {@code EntityAndGraphPropertyAccessingMethodInterceptor}, which resolves
 * it as a graph-property path off {@code VerseEntity} rather than as a raw
 * query-result column, and throws {@code IllegalStateException: Invalid
 * property '...' of bean class VerseEntity} since none of these column
 * names are real entity properties.</p>
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
