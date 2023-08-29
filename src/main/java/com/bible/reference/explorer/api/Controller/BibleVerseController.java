package com.bible.reference.explorer.api.Controller;

import static org.neo4j.driver.Values.*;

import java.util.stream.Collectors;

import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class BibleVerseController {

	@Autowired
	public Session session;

	@Autowired
	ObjectMapper objectMapper;

	@GetMapping("/api/getReferences/{verse}/{limit}")
	public String getMappingString(@PathVariable("verse") String verse, @PathVariable("limit") int limit) throws Exception {
		var variable = session.executeWrite(tx -> {
			var query = getVerseQuery(verse, limit);
			var result = tx.run(query);
			return result.list().stream().map(x -> x.keys()).collect(Collectors.toList());
		});

		return objectMapper.writeValueAsString(variable);
	}

	public Query getVerseQuery(String verseId, int limit) {
		return new Query(
			"CALL{" +
			"	MATCH (v:Verse)-[rel:references]-(p:Verse) WHERE v.title = $verseTitle" +
			"	RETURN rel" +
			"	ORDER BY rel.rank DESC" +
			"	LIMIT $limit" +
			"}" +
			"OPTIONAL MATCH (o:Verse)-[rel]-(p:Verse)" +
			"OPTIONAL MATCH (p:Verse)-[n:references]-(a:Verse)" +
			"RETURN o,rel,p,n ",
			parameters("verseTitle", verseId, "limit", limit)
		);
	}

}
