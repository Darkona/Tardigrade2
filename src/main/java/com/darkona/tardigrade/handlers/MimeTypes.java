package com.darkona.tardigrade.handlers;

final class MimeTypes {

    private MimeTypes() {
    }

    static String forFileName(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "text/html";
        } else if (name.endsWith(".css")) {
            return "text/css";
        } else if (name.endsWith(".js")) {
            return "application/javascript";
        } else if (name.endsWith(".json")) {
            return "application/json";
        } else if (name.endsWith(".xml")) {
            return "application/xml";
        } else if (name.endsWith(".csv")) {
            return "text/csv";
        } else if (name.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }

    /** For responses written inside the yaml, where there is no extension to look at. */
    static String forContent(String body) {
        if (body == null) {
            return "text/plain";
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "application/json";
        } else if (trimmed.startsWith("<")) {
            return "text/html";
        } else {
            return "text/plain";
        }
    }

    /** Text types must declare their charset; binary ones must not. */
    static String withCharset(String contentType) {
        boolean textual = contentType.startsWith("text/")
                || contentType.equals("application/json")
                || contentType.equals("application/xml")
                || contentType.equals("application/javascript");
        return textual ? contentType + "; charset=utf-8" : contentType;
    }
}
