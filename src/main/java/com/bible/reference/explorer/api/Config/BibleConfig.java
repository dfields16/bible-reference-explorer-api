package com.bible.reference.explorer.api.Config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.bible.reference.explorer.api.model.BibleBook;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableCaching
@Configuration
public class BibleConfig {

	@Value("classpath:./bibleMappings.json")
	private Resource resourceFile;

	@Autowired
	private ObjectMapper objectMapper;

	@Bean
	Map<String, BibleBook> bibleMap() throws Exception{

		return objectMapper.readValue(resourceFile.getInputStream(), new TypeReference<List<BibleBook>>(){})
									.stream()
									.collect(Collectors.toMap(x->x.getName(), Function.identity()));
	}

}
