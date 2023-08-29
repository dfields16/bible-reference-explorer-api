package com.bible.reference.explorer.api.Config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

	@Value("${neo4j.url}")
	public String url;

		@Value("${neo4j.username}")
	public String username;

		@Value("${neo4j.password}")
	public String password;

	@Bean
	public Driver driver(){
		return GraphDatabase.driver(url, AuthTokens.basic(username, password));
	}

	@Bean
	public Session session(Driver driver){
		return driver.session();
	}

}
