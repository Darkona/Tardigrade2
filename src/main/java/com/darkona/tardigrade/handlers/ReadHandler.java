package com.darkona.tardigrade.handlers;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ReadHandler extends TardigradeHandler implements HttpHandler {

    private final TardigradeConfiguration config;

    public ReadHandler(TardigradeConfiguration config) {
        this.config = config;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var path = cleanPath(exchange.getRequestURI().getRawPath());
        System.out.println("cleanedpath: " + path);
        var fileName = path.substring(path.lastIndexOf("/") + 1);
        System.out.println("FileName: " + fileName);




        if(path.lastIndexOf("/") == path.length() - 1){

            serveDirectory(exchange, new File(config.input()));
        } else {
            serveFile(exchange, new File(config.input() + "/" + fileName));
        }

        System.out.println("File name: " + fileName);

        sendResponse(exchange, "Read handler response", "text/plain");
    }

    private String cleanPath(String path) {
        return path.replaceAll("\\.\\.", "").replaceAll("/+", "/");
    }

    private void serveDirectory(HttpExchange exchange, File directory) throws IOException {
        File[] files = directory.listFiles();

        StringBuilder response = new StringBuilder("<html><body><h1>Directory Listing</h1><ul>");

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                response.append("<li><a href=\"").append(name).append("\">").append(name).append("</a></li>");
            }
        }

        response.append("</ul></body></html>");
        sendResponse(exchange, response.toString(), "text/html");
    }

    private void serveFile(HttpExchange exchange, File file) throws IOException {
        String mimeType = getMimeType(file.getName());
        sendFile(exchange, file, mimeType);
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else {
            return "application/octet-stream";
        }
    }


    private void sendFile(HttpExchange exchange, File file, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, file.length());
        OutputStream os = exchange.getResponseBody();
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        fis.close();
        os.close();
    }

    private static final String notFoundPage = "<html><head><body><h4 style=\"background-color:lightgray;margin:0;padding:0;border:0\">404 not " + "found\n" +
            "</h4><pre style=\"color:pink; background-color:grey;padding:0;margin:0\"><strong>\n" + "               ( ꒰֎꒱ ) \n" + "               උ( " + "___" +
            " )づ\n" + "               උ( ___ )づ \n" + "                උ( ___ )づ\n" + "               උ( ___ )づ\n" + " \n" + "</strong></pre>\n" + "</body" + ">\n" + "</head>\n" + "</html>";

    private void sendNotFound(HttpExchange exchange) throws IOException {
        String response = notFoundPage;
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.length());

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}