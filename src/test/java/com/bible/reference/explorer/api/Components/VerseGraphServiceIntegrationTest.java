package com.bible.reference.explorer.api.Components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.bible.reference.explorer.api.model.Neo4j.CrossReferenceResult;

/**
 * Runs {@link VerseGraphService#getReferences(String, int)} against a real,
 * in-process (Docker-free) Neo4j instance instead of a mocked one.
 *
 * <p>This exists because the Spring Data Neo4j migration shipped a
 * regression that no amount of mocking caught: the query's {@code o}/
 * {@code p} columns used to come back as real {@code Verse}-labelled nodes,
 * and Spring Data Neo4j's entity mapper throws {@code IllegalStateException:
 * More than one matching node in the record} whenever a single row carries
 * two nodes it could map to the same entity type -- a failure that only ever
 * surfaces once a query actually runs against a real database and returns a
 * multi-node row. Every earlier check in this codebase (`mvn test`, a manual
 * run against an unreachable URI) only proved the Spring wiring was correct,
 * never that a real query result could be mapped. {@link #getReferences}
 * later moved off a {@code @Query}-annotated repository method onto a
 * Cypher-DSL statement run through {@code Neo4jClient} directly, but this
 * test still exercises the same end-to-end path and the same regression
 * class, so it stays.</p>
 */
@SpringBootTest
class VerseGraphServiceIntegrationTest {

	private static Neo4j embeddedNeo4j;

	@Autowired
	private VerseGraphService verseGraphService;

	@BeforeAll
	static void startNeo4j() {
		embeddedNeo4j = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();

		try (Driver driver = GraphDatabase.driver(embeddedNeo4j.boltURI(), AuthTokens.none());
				Session session = driver.session()) {
			// Mirrors production shape: two verses with a mutual `references`
			// edge (Genesis 1:1 <-> John 1:1), plus one more hop out from John
			// 1:1 to Psalms 33:6, so the row set exercises o/p/rel/n together.
			session.run("""
					CREATE (gen:Verse {title: 'Gen.1.1', book: 'Genesis', chapter: '1', verse: '1'})
					CREATE (john:Verse {title: 'John.1.1', book: 'John', chapter: '1', verse: '1'})
					CREATE (psalms:Verse {title: 'Psalms.33.6', book: 'Psalms', chapter: '33', verse: '6'})
					CREATE (gen)-[:references {rank: 5}]->(john)
					CREATE (john)-[:references {rank: 5}]->(gen)
					CREATE (john)-[:references {rank: 3}]->(psalms)
					""")
					.consume();
		}
	}

	@AfterAll
	static void stopNeo4j() {
		if (embeddedNeo4j != null) {
			embeddedNeo4j.close();
		}
	}

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", () -> embeddedNeo4j.boltURI().toString());
		registry.add("spring.neo4j.authentication.username", () -> "");
		registry.add("spring.neo4j.authentication.password", () -> "");
	}

	@Test
	void getReferencesMapsARealMultiNodeRowWithoutThrowing() {
		CrossReferenceResult result = verseGraphService.getReferences("Gen.1.1", 10);

		assertThat(result).isNotNull();
		assertThat(result.getVerses())
				.extracting("title")
				.contains("Gen.1.1", "John.1.1");
		assertThat(result.getReferences()).isNotEmpty();

		// The mutual Gen.1.1<->John.1.1 edge must collapse to a single entry.
		long genJohnEdgeCount = result.getReferences().stream()
				.filter(ref -> {
					Long genId = result.getVerses().stream().filter(v -> "Gen.1.1".equals(v.getTitle())).findFirst().orElseThrow().getId();
					Long johnId = result.getVerses().stream().filter(v -> "John.1.1".equals(v.getTitle())).findFirst().orElseThrow().getId();
					return (ref.getFrom().equals(genId) && ref.getTo().equals(johnId))
							|| (ref.getFrom().equals(johnId) && ref.getTo().equals(genId));
				})
				.count();
		assertThat(genJohnEdgeCount).isEqualTo(1);
	}
}
