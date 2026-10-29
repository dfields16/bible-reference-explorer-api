package com.bible.reference.explorer.api.Components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class DailyStatsRecorderTest {

	private HttpServer server;
	private final AtomicReference<String> path = new AtomicReference<>();
	private final AtomicReference<String> apikey = new AtomicReference<>();
	private final AtomicReference<String> authorization = new AtomicReference<>();
	private int status = 204;

	@BeforeEach
	void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			path.set(exchange.getRequestURI().getPath());
			apikey.set(exchange.getRequestHeaders().getFirst("apikey"));
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			exchange.sendResponseHeaders(status, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private String url() {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
	}

	@Test
	void callsTheFunctionWithTheSecretKey() {
		new DailyStatsRecorder(url(), "secret").recordDailyStats();
		assertEquals("/rest/v1/rpc/record_daily_stats", path.get());
		assertEquals("secret", apikey.get());
		assertEquals("Bearer secret", authorization.get());
	}

	@Test
	void doesNothingWithoutAUrlOrKey() {
		new DailyStatsRecorder("", "secret").recordDailyStats();
		new DailyStatsRecorder(url(), "").recordDailyStats();
		assertNull(path.get());
	}

	@Test
	void anErrorStatusDoesNotThrow() {
		status = 401;
		new DailyStatsRecorder(url(), "secret").recordDailyStats();
		assertEquals("/rest/v1/rpc/record_daily_stats", path.get());
	}
}
