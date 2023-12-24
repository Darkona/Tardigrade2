package com.darkona.tardigrade.logging;

public enum Ansi {
    RESET("\u001B[0m"),
    BLACK("\u001B[1;30m"),
    DARK_BLUE("\u001B[0;34m"),
    DARK_GREEN("\u001B[0;32m"),
    DARK_AQUA("\u001B[0;36m"),
    DARK_RED("\u001B[0;31m"),
    DARK_PURPLE("\u001B[0;35m"),
    GOLD("\u001B[1;33m"),
    GRAY("\u001B[1;37m"),
    DARK_GRAY("\u001B[0;90m"),
    BLUE("\u001B[1;94m"),
    GREEN("\u001B[1;92m"),
    AQUA("\u001B[1;96m"),
    RED("\u001B[1;91m"),
    PURPLE("\u001B[0;35m"),
    PINK("\u001B[38;5;206m"),
    ORANGE("\u001B[38;5;208m"),
    LIGHT_PURPLE("\u001B[1;95m"),
    YELLOW("\u001B[1;93m"),
    WHITE("\u001B[1;97m"),
    BOLD("\u001B[1m"),
    NORMAL("\u001B[0m");

    private final String code;

    Ansi(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}