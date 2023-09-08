package com.bible.reference.explorer.api.model.BibleApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerseApiRaw {
	private VerseData data;
	private VerseMeta meta;


	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class VerseData{
		private String   id;
		private String   orgId;
		private String   bibleId;
		private String   bookId;
		private String   chapterId;
		private String   content;
		private String   reference;
		private int      verseCount;
		private String   copyright;
		private VerseRef next;
		private VerseRef previous;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class VerseRef{
		private String id;
		private String bookId;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class VerseMeta{
		private String fums;
		private String fumsId;
		private String fumsJsInclude;
		private String fumsJs;
		private String fumsNoScript;
	}
}
