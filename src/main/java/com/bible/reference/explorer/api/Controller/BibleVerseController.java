package com.bible.reference.explorer.api.Controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.bible.reference.explorer.api.Components.VerseRepository;
import com.bible.reference.explorer.api.Utils.BRECommonUtil;
import com.bible.reference.explorer.api.Utils.BibleVerseUtil;
import com.bible.reference.explorer.api.model.BibleApi.VerseApi;
import com.bible.reference.explorer.api.model.BibleApi.VerseApiRaw;
import com.bible.reference.explorer.api.model.BibleApi.VerseRequest;
import com.bible.reference.explorer.api.model.BibleApi.VerseResponse;
import com.bible.reference.explorer.api.model.Neo4j.CrossReferenceResult;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
public class BibleVerseController {

	@Autowired
	protected BibleVerseUtil bibleVerseUtil;

	@Autowired
	protected WebClient bibleWebClient;

	@Autowired
	protected VerseRepository verseRepository;

	@GetMapping("/getReferences/{verse}/{limit}")
	public CrossReferenceResult getReferences(@PathVariable("verse") String verse, @PathVariable("limit") int limit) throws Exception {
		return BRECommonUtil.timer(()-> verseRepository.getReferences(verse, limit), "Get references");
	}

	@GetMapping("/findShortestPath/{verse1}/{verse2}/{limit}")
	public CrossReferenceResult findShortestPath(@PathVariable("verse1") String v1, @PathVariable("verse2") String v2, @PathVariable("limit") int limit) throws Exception {
		return BRECommonUtil.timer(()-> verseRepository.findShortestPath(v1, v2, limit), "Get shortest path");
	}

	@GetMapping("/verify/{verse}")
	public boolean verifyVerseApi(@PathVariable("verse") String verse) throws Exception {
		try {
			bibleVerseUtil.verifyVerse(verse, true);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@PostMapping("/verses")
	public VerseResponse getVersesFromBibleAPI(@RequestBody VerseRequest req){
		List<VerseApi> verses = Flux.fromStream(req.getVerses().stream())
				.parallel()
				.map(x-> {
					try {
						return bibleVerseUtil.verifyVerse(x, false);
					} catch (Exception e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.flatMap(x -> {
					return bibleWebClient
							.get()
							.uri(uriBuilder -> uriBuilder
									.path("bibles/{bible}/verses/{verse}")
									.queryParam("content-type",            "html")
									.queryParam("include-notes",           false)
									.queryParam("include-titles",          true)
									.queryParam("include-chapter-numbers", false)
									.queryParam("include-verse-numbers",   true)
									.queryParam("include-verse-spans",     false)
									.queryParam("use-org-id",              false)
									.build(Map.of("verse", x, "bible", req.getBibleVersion().id)))
							.accept(MediaType.APPLICATION_JSON)
							.retrieve()
							.bodyToMono(VerseApiRaw.class)
							.retry(3);
				})
				.sequential()
				.toStream()
				.map(VerseApi::new)
				.collect(Collectors.toList());

		if(!CollectionUtils.isEmpty(verses)){
			return VerseResponse.builder()
				.version(req.getBibleVersion().name())
				.copyright(verses.get(0).getCopyright())
				.verses(verses)
			.build();
		}

		return null;
	}

}
