package com.darkona.tardigrade;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shows the layout an embedded server uses inside a project: mocks and their bodies live in
 * {@code src/test/resources}, next to the test that relies on them.
 */
@DisplayName("Mocks kept in the classpath")
class ClasspathMocksTest {

    private static final String GREETING = "{\"greeting\":\"hello from the classpath\"}";

    private TardigradeServer server;
    private HttpClient client;

    @BeforeEach
    void startServer() throws Exception {
        server = new TardigradeServer(new TardigradeConfiguration(new String[]{
                "-p", "0",
                "-c", TardigradeConfiguration.classpathPath("tardigrade/configuration.yml"),
                "-i", TardigradeConfiguration.classpathPath("tardigrade/input"),
                "-o", Path.of("build", "tardigrade-output").toAbsolutePath().toString(),
                "-d", "color"
        }));
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create(server.baseUrl() + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("Serves a mock body kept in src/test/resources")
    void servesAMockFromTheClasspath() throws Exception {
        HttpResponse<String> response = get("/api/greeting");

        assertEquals(200, response.statusCode());
        assertEquals(GREETING, response.body().trim());
    }

    @Test
    @DisplayName("Reads the mock table from a configuration file in the classpath")
    void readsTheConfigurationFromTheClasspath() throws Exception {
        HttpResponse<String> response = get("/api/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    @DisplayName("Records the requests, so the files on disk are only the responses")
    void recordsTheRequests() throws Exception {
        get("/api/greeting");

        assertEquals(1, server.requests().count("/api/greeting"));
    }

    @Test
    @DisplayName("Complains about a resource that is not in the classpath")
    void complainsAboutAMissingResource() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TardigradeConfiguration.classpathPath("tardigrade/nowhere.yml"));

        assertTrue(error.getMessage().contains("nowhere.yml"));
    }
}
