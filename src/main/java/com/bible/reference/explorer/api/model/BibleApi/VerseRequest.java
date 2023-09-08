package com.bible.reference.explorer.api.model.BibleApi;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerseRequest {
	ArrayList<String> verses;
}
