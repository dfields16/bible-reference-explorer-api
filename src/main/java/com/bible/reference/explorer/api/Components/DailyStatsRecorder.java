package com.bible.reference.explorer.api.Components;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Once a day, asks the usage-log database to total yesterday's usage by calling its
 * {@code record_daily_stats} function, which fills {@code daily_stats}. This used to be a GitHub
 * Actions job in the UI repo calling it with the public key; the function is now locked to the
 * service role, so only this backend (which holds the secret key) can run it. The call also keeps
 * the free Supabase project from sitting idle long enough to be paused.
 *
 * Does nothing unless both {@code supabase.url} and {@code supabase.secret-key} are set (env
 * {@code SUPABASE_URL} and {@code SUPABASE_SECRET_KEY}), so local runs don't call out. The secret
 * key must never reach the browser bundle or a log line.
 */
@Slf4j
@Component
public class DailyStatsRecorder {

	/** Long enough for a cold free-tier database, short enough not to hold a scheduler thread for ever. */
	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final String supabaseUrl;
	private final String secretKey;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

	public DailyStatsRecorder(
			@Value("${supabase.url:}") String supabaseUrl,
			@Value("${supabase.secret-key:}") String secretKey) {
		this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.replaceAll("/+$", "");
		this.secretKey = secretKey == null ? "" : secretKey;
	}

	/** 00:20 UTC, just after the day ends, which is the day {@code record_daily_stats} totals by default. */
	@Scheduled(cron = "${supabase.dailyStatsCron:0 20 0 * * *}", zone = "UTC")
	public void recordDailyStats() {
		if (supabaseUrl.isBlank() || secretKey.isBlank()) {
			log.debug("Skipping daily stats: supabase.url or supabase.secret-key is not set");
			return;
		}
		HttpRequest request = HttpRequest.newBuilder(URI.create(supabaseUrl + "/rest/v1/rpc/record_daily_stats"))
				.timeout(TIMEOUT)
				.header("apikey", secretKey)
				.header("Authorization", "Bearer " + secretKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 == 2) {
				log.info("Recorded daily stats status={}", response.statusCode());
			} else {
				log.error("Recording daily stats failed status={} body={}", response.statusCode(), response.body());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Recording daily stats was interrupted");
		} catch (IOException e) {
			log.error("Recording daily stats failed", e);
		}
	}
}
