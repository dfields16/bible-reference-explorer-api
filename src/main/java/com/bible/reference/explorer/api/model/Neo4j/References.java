package com.bible.reference.explorer.api.model.Neo4j;

import org.neo4j.driver.types.Relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class References {
	private String from;
	private String to;
	private Integer rank;

	public References(Relationship rel){
		this.from = rel.startNodeElementId();
		this.to = rel.endNodeElementId();
		this.rank = Integer.valueOf(rel.get("rank").asString());
	}
}
