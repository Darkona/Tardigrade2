package com.darkona.tardigrade.handlers;

import ch.qos.logback.classic.Logger;
import com.darkona.tardigrade.configuration.MockRoute;
import com.darkona.tardigrade.configuration.MockRouter;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Answers with the content declared in configuration.yml when the path matches a
 * mock. Anything that does not match is only logged to the console.
 */
public class MockHandler extends TardigradeHandler implements HttpHandler {

    private final TardigradeConfiguration config;
    private volatile MockRouter router;
    private final LogHandler logHandler;

    private final Logger log = (Logger) LoggerFactory.getLogger("Mocks");

    public MockHandler(TardigradeConfiguration config, MockRouter router, LogHandler logHandler) {
        this.config = config;
        this.router = router;
        this.logHandler = logHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Optional<MockRoute> match = router.match(path, exchange.getRequestMethod());

        if (match.isEmpty()) {
            logHandler.handle(exchange);
            return;
        }

        MockRoute route = match.get();
        logHandler.logRequest(exchange);
        log.info("{} matched mock {}", path, route.path());

        if (route.file() != null) {
            sendMockFile(exchange, route);
        } else {
            sendResponse(exchange, route.status(), route.body(), contentTypeFor(route, null));
        }
    }

    private void sendMockFile(HttpExchange exchange, MockRoute route) throws IOException {
        Path base = Path.of(config.input()).toAbsolutePath().normalize();
        Path file = base.resolve(route.file()).normalize();

        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            log.error("Mock {} points to a file that cannot be served: {}", route.path(), file);
            sendResponse(exchange, 500, "Mock file not found: " + route.file(), "text/plain");
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", MimeTypes.withCharset(contentTypeFor(route, route.file())));
        exchange.sendResponseHeaders(route.status(), Files.size(file));
        try (var in = Files.newInputStream(file); OutputStream os = exchange.getResponseBody()) {
            in.transferTo(os);
        }
    }

    private String contentTypeFor(MockRoute route, String fileName) {
        if (route.contentType() != null) {
            return route.contentType();
        }
        if (fileName != null) {
            return MimeTypes.forFileName(fileName);
        }
        return MimeTypes.forContent(route.body());
    }

    /** Swaps the routing table after configuration.yml changes on disk. */
    public void setRouter(MockRouter router) {
        this.router = router;
    }
}
