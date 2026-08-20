package com.darkona.tardigrade.handlers;

import ch.qos.logback.classic.Logger;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReadHandler extends TardigradeHandler implements HttpHandler {

    private final TardigradeConfiguration config;

    private final Logger log = (Logger) LoggerFactory.getLogger("File reader");

    public ReadHandler(TardigradeConfiguration config) {
        this.config = config;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Path base = Path.of(config.input()).toAbsolutePath().normalize();
        String path = exchange.getRequestURI().getPath();
        String relative = path.substring(exchange.getHttpContext().getPath().length());

        if (relative.isBlank() || relative.equals("/")) {
            serveDirectory(exchange, base);
            return;
        }

        Path requested = base.resolve(relative.startsWith("/") ? relative.substring(1) : relative).normalize();
        // Contains path traversal: everything served must live under the input directory.
        if (!requested.startsWith(base)) {
            log.warn("Rejected path outside of the input directory: {}", path);
            sendNotFound(exchange);
            return;
        }

        if (Files.isDirectory(requested)) {
            serveDirectory(exchange, requested);
        } else if (Files.isRegularFile(requested)) {
            sendFile(exchange, requested);
        } else {
            log.warn("File not found: {}", requested);
            sendNotFound(exchange);
        }
    }

    private void serveDirectory(HttpExchange exchange, Path directory) throws IOException {
        StringBuilder response = new StringBuilder("<html><body><h1>Directory Listing</h1><ul>");

        if (Files.isDirectory(directory)) {
            try (var entries = Files.list(directory)) {
                entries.forEach(entry -> {
                    String name = entry.getFileName().toString();
                    response.append("<li><a href=\"").append(name).append("\">").append(name).append("</a></li>");
                });
            }
        } else {
            log.warn("Input directory does not exist: {}", directory);
        }

        response.append("</ul></body></html>");
        sendResponse(exchange, response.toString(), "text/html");
    }

    private void sendFile(HttpExchange exchange, Path file) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", MimeTypes.withCharset(MimeTypes.forFileName(file.getFileName().toString())));
        exchange.sendResponseHeaders(200, Files.size(file));
        try (InputStream in = Files.newInputStream(file); OutputStream os = exchange.getResponseBody()) {
            in.transferTo(os);
        }
    }

    private static final String notFoundPage = "<html><head><body><h4 style=\"background-color:lightgray;margin:0;padding:0;border:0\">404 not " + "found\n" +
            "</h4><pre style=\"color:pink; background-color:grey;padding:0;margin:0\"><strong>\n" + "               ( ꒰֎꒱ ) \n" + "               උ( " + "___" +
            " )づ\n" + "               උ( ___ )づ \n" + "                උ( ___ )づ\n" + "               උ( ___ )づ\n" + " \n" + "</strong></pre>\n" + "</body" + ">\n" + "</head>\n" + "</html>";

    private void sendNotFound(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 404, notFoundPage, "text/html");
    }
}
