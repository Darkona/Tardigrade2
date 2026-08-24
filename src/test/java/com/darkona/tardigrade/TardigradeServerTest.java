package com.darkona.tardigrade;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.recording.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Embedded Tardigrade server")
class TardigradeServerTest {

    private static final String JSON_BODY = "{\"client\":42,\"name\":\"Nandu\"}";
    private static final String MOCK_RESPONSE = "{\"id\":42,\"status\":\"OK\"}";
    private static final String CORRELATION_HEADER = "X-CORRELACION-ID";

    @TempDir
    Path folder;

    private TardigradeServer server;
    private HttpClient client;

    @BeforeEach
    void startServer() throws IOException {
        Path input = Files.createDirectories(folder.resolve("input"));
        Files.writeString(input.resolve("client.json"), MOCK_RESPONSE);
        Files.writeString(folder.resolve("configuration.yml"),
                "mocks:\n  - path: /api/clients/42\n    file: client.json\n");

        server = new TardigradeServer(configuration(input));
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private TardigradeConfiguration configuration(Path input) {
        try {
            return new TardigradeConfiguration(new String[]{
                    "-p", "0",
                    "-c", folder.resolve("configuration.yml").toString(),
                    "-i", input.toString(),
                    "-o", folder.resolve("output").toString(),
                    "-d", "color"
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create(server.baseUrl() + path)).build();
    }

    @Nested
    @DisplayName("Life cycle")
    class LifeCycle {

        @Test
        @DisplayName("Takes a free port when configured with port 0")
        void takesAFreePort() {
            assertTrue(server.port() > 0, "Should have taken a real port");
            assertTrue(server.isRunning());
            assertEquals("http://localhost:" + server.port(), server.baseUrl());
        }

        @Test
        @DisplayName("Refuses to start the same instance twice")
        void refusesToStartTwice() {
            assertThrows(IllegalStateException.class, () -> server.start());
        }

        @Test
        @DisplayName("Releases the port on stop and can be started again")
        void releasesThePortOnStop() throws IOException {
            server.stop();

            assertFalse(server.isRunning());
            assertEquals(-1, server.port());

            server.start();

            assertTrue(server.isRunning());
            assertTrue(server.port() > 0, "Should have bound again");
        }

        @Test
        @DisplayName("Two instances share a JVM on different ports")
        void twoInstancesShareAJvm() throws IOException {
            TardigradeServer other = new TardigradeServer(configuration(folder.resolve("input")));
            try {
                other.start();
                assertNotEquals(server.port(), other.port());
            } finally {
                other.stop();
            }
        }
    }

    @Nested
    @DisplayName("Request recording")
    class RequestRecording {

        @Test
        @DisplayName("Records the method, path and body it receives")
        void recordsTheRequest() throws Exception {
            send(HttpRequest.newBuilder(URI.create(server.baseUrl() + "/api/payments"))
                    .POST(HttpRequest.BodyPublishers.ofString(JSON_BODY))
                    .build());

            RecordedRequest received = server.requests().last().orElseThrow();

            assertEquals("POST", received.method());
            assertEquals("/api/payments", received.path());
            assertEquals(JSON_BODY, received.body());
        }

        @Test
        @DisplayName("Keeps headers and looks them up ignoring case")
        void keepsHeaders() throws Exception {
            send(HttpRequest.newBuilder(URI.create(server.baseUrl() + "/api/payments"))
                    .header(CORRELATION_HEADER, "abc-123")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON_BODY))
                    .build());

            RecordedRequest received = server.requests().last().orElseThrow();

            assertTrue(received.hasHeader(CORRELATION_HEADER));
            assertEquals(Optional.of("abc-123"), received.header("x-correlacion-id"));
        }

        @Test
        @DisplayName("Splits the query string off the path")
        void splitsTheQueryString() throws Exception {
            send(get("/api/clients?active=true"));

            RecordedRequest received = server.requests().last().orElseThrow();

            assertEquals("/api/clients", received.path());
            assertEquals("active=true", received.query());
        }

        @Test
        @DisplayName("Counts requests per path")
        void countsPerPath() throws Exception {
            send(get("/api/one"));
            send(get("/api/two"));
            send(get("/api/one"));

            assertEquals(3, server.requests().count());
            assertEquals(2, server.requests().count("/api/one"));
            assertEquals(1, server.requests().count("/api/two"));
        }

        @Test
        @DisplayName("Is empty once cleared")
        void isEmptyOnceCleared() throws Exception {
            send(get("/api/one"));

            server.requests().clear();

            assertTrue(server.requests().isEmpty());
        }
    }

    @Nested
    @DisplayName("Defined responses")
    class DefinedResponses {

        @Test
        @DisplayName("Answers with the file configured for the path")
        void answersWithTheMock() throws Exception {
            HttpResponse<String> response = send(get("/api/clients/42"));

            assertEquals(200, response.statusCode());
            assertEquals(MOCK_RESPONSE, response.body());
        }

        @Test
        @DisplayName("Records mocked requests as well")
        void recordsMockedRequests() throws Exception {
            send(get("/api/clients/42"));

            assertEquals(1, server.requests().count("/api/clients/42"));
        }

        @Test
        @DisplayName("Acknowledges paths with no mock defined")
        void acknowledgesUnmappedPaths() throws Exception {
            HttpResponse<String> response = send(get("/path/with/no/mock"));

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("logged"));
        }
    }
}
