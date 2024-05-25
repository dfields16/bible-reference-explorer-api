package com.bible.reference.explorer.api.Utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.bible.reference.explorer.api.model.BibleApi.VerseApi;
import com.bible.reference.explorer.api.model.BibleApi.VerseRequest;

@Component
public class BibleAPIFacade {

	@Autowired
	protected BibleVerseUtil bibleVerseUtil;


	public List<VerseApi> getVerses(VerseRequest req){
		req.getVerses().stream().map(x-> {
			try {
				return bibleVerseUtil.verifyVerse(x, false);
			} catch (Exception e) {
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toSet());
		return List.of();
	}



	@Cacheable("verses")
	public VerseApi getVerseById(String verseId, String version){


		return null;
	}

}
