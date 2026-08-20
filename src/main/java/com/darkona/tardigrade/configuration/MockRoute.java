package com.darkona.tardigrade.configuration;

import java.util.Map;

/**
 * A canned response declared in configuration.yml.
 *
 * @param path        path to intercept; acts as a prefix when it ends with an asterisk.
 * @param method      HTTP verb it is limited to, or null for any.
 * @param file        file holding the response, relative to the input directory.
 * @param body        response written straight into the yaml, an alternative to file.
 * @param status      response code.
 * @param contentType explicit Content-Type, or null to deduce it.
 */
public record MockRoute(String path, String method, String file, String body, int status, String contentType) {

    public static MockRoute from(Map<?, ?> raw) {
        String path = string(raw.get("path"));
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("A mock needs a path: " + raw);
        }
        String file = string(raw.get("file"));
        String body = string(raw.get("body"));
        if (file == null && body == null) {
            throw new IllegalArgumentException("Mock " + path + " needs either a file or a body.");
        }
        String method = string(raw.get("method"));
        return new MockRoute(
                path,
                method == null ? null : method.toUpperCase(),
                file,
                body,
                raw.get("status") instanceof Number n ? n.intValue() : 200,
                string(raw.get("contentType"))
        );
    }

    public boolean isWildcard() {
        return path.endsWith("*");
    }

    /** Fixed part of a wildcard path: /clientes/* returns /clientes/. */
    public String prefix() {
        return path.substring(0, path.length() - 1);
    }

    public boolean acceptsMethod(String requestMethod) {
        return method == null || method.equalsIgnoreCase(requestMethod);
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
