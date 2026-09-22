package com.bible.reference.explorer.api.model.Neo4j;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The {@code references} relationship between two {@link VerseEntity} nodes,
 * modelled as a proper Spring Data Neo4j relationship entity so the
 * {@code rank} carried on the edge is mapped by SDN itself instead of being
 * hand-parsed off a raw driver {@code Relationship}.
 */
@RelationshipProperties
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseReference {

	@RelationshipId
	private Long id;

	private Integer rank;

	@TargetNode
	private VerseEntity verse;
}
