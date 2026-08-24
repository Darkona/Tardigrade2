package com.darkona.tardigrade.recording;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A request as it arrived, kept in memory so a test can assert on it instead of
 * reading the console.
 *
 * @param method     HTTP verb, uppercase.
 * @param path       path with no query string.
 * @param query      raw query string, or null when there was none.
 * @param headers    request headers, keyed as the server received them.
 * @param body       body as text; empty when the request had none.
 * @param receivedAt instant the request reached the server.
 */
public record RecordedRequest(String method, String path, String query,
                              Map<String, List<String>> headers, String body, Instant receivedAt) {

    /** First value of a header, matched without case sensitivity. */
    public Optional<String> header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .filter(values -> !values.isEmpty())
                .map(List::getFirst)
                .findFirst();
    }

    public boolean hasHeader(String name) {
        return header(name).isPresent();
    }

    public boolean is(String method, String path) {
        return this.method.equalsIgnoreCase(method) && this.path.equals(path);
    }

    @Override
    public String toString() {
        return method.toUpperCase(Locale.ROOT) + " " + path + (query == null ? "" : "?" + query);
    }
}
