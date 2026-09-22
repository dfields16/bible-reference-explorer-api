package com.bible.reference.explorer.api.Repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.bible.reference.explorer.api.model.Neo4j.VerseEntity;

/**
 * Spring Data Neo4j repository for {@link VerseEntity}, replacing the raw
 * driver {@code Session}/hand-written Cypher strings the app used to run
 * directly.
 *
 * <p>Carries no custom query methods of its own: the reference-graph and
 * shortest-path traversals aren't shapes the derived-query mechanism can
 * produce, and (per {@code VerseGraphService}'s class-level Javadoc) can't
 * be expressed as {@code @Query} methods here either, so they're built and
 * run directly against {@link org.springframework.data.neo4j.core.Neo4jClient}
 * in {@code com.bible.reference.explorer.api.Components.VerseGraphService}
 * instead. This interface still exists as the real Spring Data Neo4j
 * repository backing {@link VerseEntity} -- standard CRUD
 * (findById/save/etc.), and the base for any future derived-query method
 * that *does* fit the declarative {@code @Query}/derived-query shape.</p>
 */
public interface VerseRepository extends Neo4jRepository<VerseEntity, Long> {
}
