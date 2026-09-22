package com.bible.reference.explorer.api.model.Neo4j;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Spring Data Neo4j persistence entity for a single {@code Verse} node.
 *
 * <p>Kept separate from {@link Verse}, which is the flat DTO returned over
 * REST: {@code label} and {@code level} on the old hand-mapped POJO were
 * always derived/display values rather than graph properties, so they have
 * no place on the entity itself. {@link Verse#of(VerseEntity)} does that
 * derivation when shaping a response.</p>
 */
@Node("Verse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseEntity {

	@Id
	@GeneratedValue
	private Long id;

	private String title;
	private String book;
	private String chapter;
	private String verse;

	/**
	 * The outgoing {@code references} edges to other verses, each carrying a
	 * {@code rank}. Modelled as {@link RelationshipProperties} rather than a
	 * flat, disconnected DTO so the rank on the edge is a first-class part of
	 * the domain graph (e.g. populated automatically when this entity is
	 * loaded via {@code Neo4jRepository#findById}).
	 */
	@Relationship(type = "references", direction = Relationship.Direction.OUTGOING)
	@Builder.Default
	private Set<VerseReference> references = new HashSet<>();
}
