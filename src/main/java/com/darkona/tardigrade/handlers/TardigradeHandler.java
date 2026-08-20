package com.darkona.tardigrade.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TardigradeHandler {

    protected void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        sendResponse(exchange, 200, response, contentType);
    }

    protected void sendResponse(HttpExchange exchange, int status, String response, String contentType) throws IOException {
        // Content-Length counts bytes, not characters: with multibyte UTF-8,
        // response.length() truncates the response.
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type",
                contentType.contains("charset") ? contentType : MimeTypes.withCharset(contentType));
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
