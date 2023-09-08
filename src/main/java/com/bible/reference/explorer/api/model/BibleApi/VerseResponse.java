package com.bible.reference.explorer.api.model.BibleApi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerseResponse {
	private String version;
	private List<VerseApi> verses;
	private String copyright;
}
