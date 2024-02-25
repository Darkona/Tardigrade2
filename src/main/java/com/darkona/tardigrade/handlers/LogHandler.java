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

import static com.darkona.tardigrade.Main.rainbowify;

public class LogHandler extends TardigradeHandler implements HttpHandler {

    Logger headLog = (Logger) LoggerFactory.getLogger(rainbowify("HEADER"));
    Logger bodyLog = (Logger) LoggerFactory.getLogger("BODY");
    Logger methodLog = (Logger) LoggerFactory.getLogger("METHOD");

    private final TardigradeConfiguration config;

    public LogHandler(TardigradeConfiguration config) {
        this.config = config;
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
        logMethod(exchange.getRequestMethod());
        logHeaders(exchange.getRequestHeaders());
        logBody(exchange.getRequestBody());
        sendResponse(exchange, "You request has been logged! :)", "text/plain");
    }

    private void logBody(InputStream body)  {
        try(body){
            bodyLog.info("\n" + new String(body.readAllBytes(), StandardCharsets.UTF_8));
        }catch(IOException e){
            bodyLog.error("Error reading body!", e);
        }

    }
    private void logMethod(String requestMethod) {
        String bold = "\u001B[1m";
        if (config.color()) {
            methodLog.info(bold + HttpMethodWordColor.valueOf(requestMethod.toUpperCase()).color + requestMethod + RegularAnsi.RESET);
        } else {
            methodLog.info(requestMethod);
        }
    }

    enum HttpMethodWordColor {
        GET(BoldAnsi.GREEN),
        POST(BoldAnsi.GOLD),
        PUT(BoldAnsi.BLUE),
        PATCH(BoldAnsi.PURPLE),
        DELETE(BoldAnsi.RED),
        HEAD(BoldAnsi.GREEN),
        OPTIONS(BoldAnsi.PINK);

        private final BoldAnsi color;

        HttpMethodWordColor(BoldAnsi color) {
            this.color = color;
        }
    }
}
