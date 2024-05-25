package com.bible.reference.explorer.api.Utils;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bible.reference.explorer.api.model.BibleBook;

@Component
public class BibleVerseUtil {

	@Autowired
	protected Map<String, BibleBook> bibleMap;

	public int verifyLimit(int limit) {
		if (limit <= 10) {
			return 10;
		} else if (limit >= 50) {
			return 50;
		}
		return limit;
	}

	public String verifyVerse(String bibleRef, boolean isDbKey) throws Exception {
		String[] parts = bibleRef.split("\\.");
		String book = parts[0];
		Integer chapter = Integer.valueOf(parts[1]);
		Integer verse = Integer.valueOf(parts[2]);

		BibleBook bibleBook = bibleMap.get(book);
		if (0 < chapter && chapter <= bibleBook.getChapterCount()) {
			if (0 < verse && verse <= bibleBook.getVerseCountList().get(chapter - 1)) {
				return (isDbKey ? bibleBook.getDbKey() : bibleBook.getApiKey()) + "." + chapter + "." + verse;
			}
		}
		throw new Exception("Invalid Bible reference.");
	}

}
