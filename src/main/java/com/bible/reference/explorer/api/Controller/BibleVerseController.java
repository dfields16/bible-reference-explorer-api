package com.bible.reference.explorer.api.Controller;

import static org.neo4j.driver.Values.*;

import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@Controller
public class BibleVerseController {

	@Autowired
	public Session session;

	@GetMapping("/api/getReferences/{verse}")
	public String getMappingString(@PathVariable("verse") String verseId){
		var variable = session.executeWrite(tx -> {
			var query = new Query("CREATE (a:Greeting) SET a.message = $verseId RETURN a.message + ', from node ' + id(a)", parameters("verseId", verseId));
			var result = tx.run(query);
			return result.list().stream().map(x->x.toString()).reduce("", String::concat);
	  });

	  return "";
	}


}
