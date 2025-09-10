package com.bible.reference.explorer.api.model.Neo4j;

import org.neo4j.driver.types.Node;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Verse {
	private String id;
	private String title;
	private String book;
	private String chapter;
	private String verse;
	private String label;
	private String level;

	public Verse(Node node){
		this.id = node.elementId();
		this.title = node.get("title").asString();
		this.book = node.get("book").asString();
		this.chapter = node.get("chapter").asString();
		this.verse = node.get("verse").asString();
		this.label = book + " " + chapter + ":" + verse;
	}
}
