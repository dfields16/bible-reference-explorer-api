package com.bible.reference.explorer.api.Repository;

/**
 * Interface projection for a {@code references} edge returned by a custom
 * {@link VerseRepository} query, as a plain {@code {rank, from, to}} map
 * literal rather than a raw driver {@code Relationship}.
 */
public interface VerseEdge {
	Integer getRank();
	Long getFrom();
	Long getTo();
}
