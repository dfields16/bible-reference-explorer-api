package com.bible.reference.explorer.api.model.Neo4j;

import java.util.Objects;

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
		try {
			this.rank = rel.get("rank").asInt();
		} catch (Exception e) {
			this.rank = 0;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		References other = (References) obj;
		return Objects.equals(from, other.from) && Objects.equals(to, other.to);
	}
}
