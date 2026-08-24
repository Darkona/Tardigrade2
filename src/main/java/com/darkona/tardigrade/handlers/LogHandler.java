package com.darkona.tardigrade.handlers;

import ch.qos.logback.classic.Logger;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.logging.BoldAnsi;
import com.darkona.tardigrade.logging.RegularAnsi;
import com.darkona.tardigrade.recording.RecordedRequest;
import com.darkona.tardigrade.recording.RequestLog;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static com.darkona.tardigrade.logging.Rainbow.rainbowify;

public class LogHandler extends TardigradeHandler implements HttpHandler {

    private final Logger headLog;
    private final Logger bodyLog = (Logger) LoggerFactory.getLogger("BODY");
    private final Logger methodLog = (Logger) LoggerFactory.getLogger("METHOD");

    private final TardigradeConfiguration config;
    private final RequestLog requests;

    public LogHandler(TardigradeConfiguration config) {
        this(config, new RequestLog());
    }

    public LogHandler(TardigradeConfiguration config, RequestLog requests) {
        this.config = config;
        this.requests = requests;
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

    /** Records and logs the request without answering it, so the mocker can reuse it. */
    public void logRequest(HttpExchange exchange) {
        // The body is always read, even when it is not printed: the recording needs it and the
        // stream can only be drained once.
        String body = readBody(exchange.getRequestBody());
        requests.record(recordOf(exchange, body));

        logMethod(exchange.getRequestMethod(), exchange.getRequestURI().toString());
        if (config.headers()) {
            logHeaders(exchange.getRequestHeaders());
        }
        if (config.body()) {
            bodyLog.info("\n" + body);
        }
    }

    private RecordedRequest recordOf(HttpExchange exchange, String body) {
        return new RecordedRequest(
                exchange.getRequestMethod().toUpperCase(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                Map.copyOf(exchange.getRequestHeaders()),
                body,
                Instant.now()
        );
    }

    private String readBody(InputStream body) {
        try (body) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            bodyLog.error("Error reading body!", e);
            return "";
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
