package com.bible.reference.explorer.api.model.Neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat REST-response shape for a single {@code references} edge between two
 * verses, returned inside {@link CrossReferenceResult}.
 *
 * <p>{@code from}/{@code to} carry the SDN-generated
 * {@link VerseEntity#getId()} of the endpoints, matching {@link Verse#getId()}
 * on the "verses" side of the same result.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class References {
	private Long from;
	private Long to;
	private Integer rank;
}
