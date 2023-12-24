package com.darkona.tardigrade.handlers;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class WriteHandler extends TardigradeHandler implements HttpHandler {

    private TardigradeConfiguration configuration;
    public WriteHandler(TardigradeConfiguration config) {
        this.configuration = config;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        sendResponse(exchange, "Write handler", "text/plain");
    }
}
