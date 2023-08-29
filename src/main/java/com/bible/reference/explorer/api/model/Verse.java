package com.bible.reference.explorer.api.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import lombok.Data;

@Data
@Node
public class Verse {

	@Id
	private long id;

	@Property("title")
	private String title;

	@Property("book")
	private String book;

	@Property("chapter")
	private String chapter;

	@Property("verse")
	private String verse;

}
