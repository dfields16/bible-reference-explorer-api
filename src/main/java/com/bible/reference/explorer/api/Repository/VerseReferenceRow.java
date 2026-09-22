package com.bible.reference.explorer.api.Repository;

import com.bible.reference.explorer.api.model.Neo4j.VerseEntity;

/**
 * Interface projection for one row of
 * {@link VerseRepository#findReferenceGraph(String, int)}: the two verse
 * endpoints ({@code o}/{@code p}) of the top-ranked reference edges off the
 * requested verse, plus the edge itself ({@code rel}) and one further edge
 * one hop out ({@code n}). Mirrors the shape the hand-written Cypher used to
 * return as raw driver {@code Node}/{@code Relationship} values.
 */
public interface VerseReferenceRow {
	VerseEntity getO();
	VerseEdge getRel();
	VerseEntity getP();
	VerseEdge getN();
}
