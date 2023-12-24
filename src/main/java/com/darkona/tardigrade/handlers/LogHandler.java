package com.darkona.tardigrade.handlers;

import ch.qos.logback.classic.Logger;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LogHandler extends TardigradeHandler implements HttpHandler {

    Logger headLog = (Logger) LoggerFactory.getLogger("HEADER");
    Logger bodyLog = (Logger) LoggerFactory.getLogger("BODY");
    Logger methodLog = (Logger) LoggerFactory.getLogger("METHOD");

    private final TardigradeConfiguration config;
    public LogHandler(TardigradeConfiguration config) {
        this.config = config;

    }

    private void logHeaders(HttpExchange exchange){
        StringBuilder sb = new StringBuilder("\n");
        exchange.getRequestHeaders().forEach(
                (key, value) -> sb.append(key).append(" : ").append(String.join(",", value)).append("\n")
        );
        headLog.info(sb.toString());
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        methodLog.info(exchange.getRequestMethod());
        logHeaders(exchange);
        bodyLog.info("\n" + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        sendResponse(exchange, "Log handler", "text/plain");
    }
}
