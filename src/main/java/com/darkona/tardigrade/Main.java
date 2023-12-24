package com.darkona.tardigrade;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.handlers.ReadHandler;
import com.darkona.tardigrade.handlers.LogHandler;
import com.darkona.tardigrade.handlers.WriteHandler;
import com.darkona.tardigrade.logging.Ansi;
import com.sun.net.httpserver.HttpServer;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;


public class Main {

    static final String name = "Tardigrade";
    static final String version = "1.0";
    static final String icon = "( ꒰֎꒱ )";
    private static final String banner = "\u001B[1m\n (꒰֎ ꒱) \n උ( ___ )づ\n උ( ___ )づ \n  උ( ___ )づ\n උ( ___ )づ\u001B[0m";
    private static final String nonUtfbanner = "\u001B[1m\n ( {∞} ) \n Ç( ___ )P\n Ç( ___ )P \n  Ç( ___ )P\n Ç( ___ )P\u001B[0m";
    private static final String initMessage = name + " Server version " + version + " initializing.";

    private static TardigradeConfiguration config;

    static final String fullColor = Ansi.RED + "F"
            + Ansi.ORANGE + "u"
            + Ansi.YELLOW + "l"
            + Ansi.GREEN + "l "
            + Ansi.AQUA + "c"
            + Ansi.BLUE + "o"
            + Ansi.DARK_BLUE + "l"
            + Ansi.PURPLE + "o"
            + Ansi.PINK + "r" + Ansi.RESET;

    static final String monochrome = "░▒▓Monochrome▓▒░";
    public static void main(String[] args) throws Exception {
        config = new TardigradeConfiguration(args);
        if (config.color() && !AnsiConsole.isInstalled() && System.console() != null) {
            AnsiConsole.systemInstall();
        }

        printInitialization(config);

        startserver();

        System.out.println(name() + " Server is running from http://" + InetAddress.getLocalHost().getHostAddress() + ":" + config.port());
        say("Also: http://localhost:" + config.port());

    }

    private static void startserver() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(config.port())), 0);
        server.createContext("/read", new ReadHandler(config));
        server.createContext("/write", new WriteHandler(config));
        server.createContext("/log", new LogHandler(config));

        server.start();
    }

    private static String name() {
        if (config.color()) {
            return Ansi.PINK + name + Ansi.RESET;
        }
        return name;
    }

    private static void printInitialization(TardigradeConfiguration config) throws Exception {

        var actual_banner = detectUTF8() ? banner : nonUtfbanner;
        say(config.color() ? (Ansi.PINK + actual_banner + Ansi.RESET) : actual_banner);
        say(initMessage);
        say((config.color() ? fullColor : monochrome) + " logging enabled.");
        var input = (config.color() ? Ansi.DARK_GREEN : "") + "/" + config.input() + Ansi.RESET;
        var output = (config.color() ? Ansi.DARK_RED : "") + "/" + config.output() + Ansi.RESET;
        say("Reading files from: " + input + ", writing files to: " + output);
        say("Attempting to bind to port " + config.port());
    }

    private static boolean detectUTF8() {
        var console = System.console();
        if (console == null) return true;
        say("Console detected with charset = " + console.charset());
        return console.charset().equals(StandardCharsets.UTF_8);
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void say(Object o) {
        if (!config.quiet()) {
            System.out.println(o);
        }

    }
}