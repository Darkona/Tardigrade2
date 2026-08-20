package com.darkona.tardigrade.handlers;

import ch.qos.logback.classic.Logger;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.logging.BoldAnsi;
import com.darkona.tardigrade.logging.RegularAnsi;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.darkona.tardigrade.Main.rainbowify;

public class LogHandler extends TardigradeHandler implements HttpHandler {

    private final Logger headLog;
    private final Logger bodyLog = (Logger) LoggerFactory.getLogger("BODY");
    private final Logger methodLog = (Logger) LoggerFactory.getLogger("METHOD");

    private final TardigradeConfiguration config;

    public LogHandler(TardigradeConfiguration config) {
        this.config = config;
        headLog = (Logger) LoggerFactory.getLogger(config.color() ? rainbowify("HEADER") : "HEADER");
    }

    private void logHeaders(Headers headers) {
        StringBuilder sb = new StringBuilder("\n");
        headers.forEach((key, value) ->
                sb
                        .append(key)
                        .append(" : ")
                        .append(String.join(",", value))
                        .append("\n")
        );
        headLog.info(sb.toString());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logRequest(exchange);
        sendResponse(exchange, "You request has been logged! :)", "text/plain");
    }

    /** Logs the request without answering it, so the mocker can reuse it. */
    public void logRequest(HttpExchange exchange) {
        logMethod(exchange.getRequestMethod(), exchange.getRequestURI().toString());
        if (config.headers()) {
            logHeaders(exchange.getRequestHeaders());
        }
        if (config.body()) {
            logBody(exchange.getRequestBody());
        }
    }

    private void logBody(InputStream body) {
        try (body) {
            bodyLog.info("\n" + new String(body.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            bodyLog.error("Error reading body!", e);
        }

    }

    private void logMethod(String requestMethod, String uri) {
        if (config.color()) {
            methodLog.info(colorFor(requestMethod) + requestMethod + RegularAnsi.RESET + " " + uri);
        } else {
            methodLog.info(requestMethod + " " + uri);
        }
    }

    // A mocker receives any verb, including non-standard ones: without a default
    // this blew up with IllegalArgumentException and killed the request.
    private static BoldAnsi colorFor(String method) {
        return METHOD_COLORS.getOrDefault(method.toUpperCase(), BoldAnsi.AQUA);
    }

    private static final Map<String, BoldAnsi> METHOD_COLORS = Map.of(
            "GET", BoldAnsi.GREEN,
            "POST", BoldAnsi.GOLD,
            "PUT", BoldAnsi.BLUE,
            "PATCH", BoldAnsi.PURPLE,
            "DELETE", BoldAnsi.RED,
            "HEAD", BoldAnsi.GREEN,
            "OPTIONS", BoldAnsi.PINK,
            "TRACE", BoldAnsi.YELLOW
    );
}
