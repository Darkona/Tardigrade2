package com.darkona.tardigrade.handlers;

import ch.qos.logback.classic.Logger;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class WriteHandler extends TardigradeHandler implements HttpHandler {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final TardigradeConfiguration configuration;

    private final Logger log = (Logger) LoggerFactory.getLogger("File writer");

    public WriteHandler(TardigradeConfiguration config) {
        this.configuration = config;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (!WRITE_METHODS.contains(method)) {
            exchange.getResponseHeaders().set("Allow", String.join(", ", WRITE_METHODS));
            sendResponse(exchange, 405, "Write accepts POST, PUT or PATCH.", "text/plain");
            return;
        }

        Path base = Path.of(configuration.output()).toAbsolutePath().normalize();
        String path = exchange.getRequestURI().getPath();
        String relative = path.substring(exchange.getHttpContext().getPath().length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }

        String name = relative.isBlank() ? defaultName(exchange) : relative;
        Path target = base.resolve(name).normalize();
        // Same rule as reading: nothing is written outside the output directory.
        if (!target.startsWith(base)) {
            log.warn("Rejected path outside of the output directory: {}", path);
            sendResponse(exchange, 400, "Bad path.", "text/plain");
            return;
        }

        Files.createDirectories(target.getParent());
        long bytes;
        try (InputStream body = exchange.getRequestBody();
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bytes = body.transferTo(out);
        }

        log.info("Wrote {} bytes to {}", bytes, target);
        sendResponse(exchange, 201, "Written " + bytes + " bytes to " + base.relativize(target) + "\n", "text/plain");
    }

    private String defaultName(HttpExchange exchange) {
        return "request-" + LocalDateTime.now().format(STAMP) + extensionFor(exchange.getRequestHeaders().getFirst("Content-Type"));
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            return ".txt";
        }
        String type = contentType.toLowerCase();
        if (type.contains("json")) {
            return ".json";
        } else if (type.contains("xml")) {
            return ".xml";
        } else if (type.contains("html")) {
            return ".html";
        } else if (type.contains("csv")) {
            return ".csv";
        } else {
            return ".txt";
        }
    }
}
