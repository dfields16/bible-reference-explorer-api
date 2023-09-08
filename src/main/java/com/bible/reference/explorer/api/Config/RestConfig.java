package com.bible.reference.explorer.api.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestConfig implements WebMvcConfigurer {

	@Value("${bibleApi.token}")
	private String bibleApiToken;

	@Value("${bibleApi.baseUrl}")
	private String bibleBaseUrl;

	@Bean
	public WebClient bibleWebClient() {
		return WebClient.builder()
				.baseUrl(bibleBaseUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("api-key", bibleApiToken)
				.build();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("*");
	}
}
