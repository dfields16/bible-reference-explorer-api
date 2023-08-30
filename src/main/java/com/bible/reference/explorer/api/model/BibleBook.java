package com.bible.reference.explorer.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BibleBook {
	private String name;
	private String group;
	private String apiKey;
	private String dbKey;
	private Integer chapterCount;
	private List<Integer> verseCountList;
}
