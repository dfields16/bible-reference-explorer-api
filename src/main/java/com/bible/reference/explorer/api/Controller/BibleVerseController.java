package com.bible.reference.explorer.api.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BibleVerseController {

	@GetMapping("/api/get")
	public String getMappingString(){
		return "";
	}


}
