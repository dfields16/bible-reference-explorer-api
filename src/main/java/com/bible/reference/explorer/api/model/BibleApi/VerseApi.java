package com.bible.reference.explorer.api.model.BibleApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties
public class VerseApi {
	private String shortReference;
	private String reference;
	private String html;
	private String book;
	private String chapter;
	private String copyright;

	public VerseApi(VerseApiRaw raw) {
		shortReference = raw.getData().getId();
		reference      = raw.getData().getReference();
		html           = raw.getData().getContent();
		book           = raw.getData().getBookId();
		chapter        = raw.getData().getChapterId();
		copyright      = raw.getData().getCopyright();
	}
}
