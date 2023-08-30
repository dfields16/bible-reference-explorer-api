package com.bible.reference.explorer.api.model;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrossReferenceResult {
	Set<Verse> verses;
	Set<References> references;
}
