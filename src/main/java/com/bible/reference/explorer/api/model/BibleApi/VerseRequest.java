package com.bible.reference.explorer.api.model.BibleApi;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerseRequest {
	BibleVersion bibleVersion;
	ArrayList<String> verses;

	public static enum BibleVersion{
		KJV("de4e12af7f28f599-02"),
		ASV("06125adad2d5898a-01");

		public String id;

		private BibleVersion(String id){
			this.id = id;
		}
	}
}
