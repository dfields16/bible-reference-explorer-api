package com.bible.reference.explorer.api.model.Neo4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat REST-response shape for a verse, returned inside
 * {@link CrossReferenceResult}.
 *
 * <p>This is deliberately not the {@code @Node}-annotated persistence entity
 * ({@link VerseEntity}): {@code label} is only a display string derived from
 * book/chapter/verse and {@code level} is only meaningful on shortest-path
 * results (the verse's distance from the start of the path), so neither is a
 * real graph property.</p>
 *
 * <p>{@code id} carries the SDN-generated internal id of the corresponding
 * {@link VerseEntity} (a number) rather than the previous Neo4j driver
 * {@code elementId()} string; see the migration notes for details.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Verse {
	private Long id;
	private String title;
	private String book;
	private String chapter;
	private String verse;
	private String label;
	private String level;

	public static Verse of(VerseEntity entity) {
		return Verse.builder()
				.id(entity.getId())
				.title(entity.getTitle())
				.book(entity.getBook())
				.chapter(entity.getChapter())
				.verse(entity.getVerse())
				.label(entity.getBook() + " " + entity.getChapter() + ":" + entity.getVerse())
				.build();
	}
}
