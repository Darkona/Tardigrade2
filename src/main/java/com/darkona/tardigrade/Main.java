package com.darkona.tardigrade;

import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.handlers.LogHandler;
import com.darkona.tardigrade.handlers.ReadHandler;
import com.darkona.tardigrade.handlers.WriteHandler;
import com.darkona.tardigrade.logging.BoldAnsi;
import com.darkona.tardigrade.logging.RegularAnsi;
import com.sun.net.httpserver.HttpServer;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class Main {

    static final String NAME = "Tardigrade";
    static final String VERSION = "1.0";
    static final String icon = "( ꒰֎꒱ )";
    static final String monochrome = "░▒▓Monochrome▓▒░";
    private static final String BANNER = "\u001B[1m\n (꒰֎ ꒱) \n උ( ___ )づ\n උ( ___ )づ \n  උ( ___ )づ\n උ( ___ )づ\u001B[0m";
    private static final String NON_UTF_BANNER = "\u001B[1m\n ( {∞} ) \n Ç( ___ )P\n Ç( ___ )P \n  Ç( ___ )P\n Ç( ___ )P\u001B[0m";
    private static final String INIT_MESSAGE = NAME + " Server version " + VERSION + " initializing.";
    private static TardigradeConfiguration config;
    static final String fullColor = rainbowify("Full color");


    public static String rainbowify(String s) {
        List<BoldAnsi> rainbow = List.of(BoldAnsi.RED, BoldAnsi.ORANGE, BoldAnsi.YELLOW, BoldAnsi.GREEN, BoldAnsi.AQUA, BoldAnsi.BLUE, BoldAnsi.PURPLE, BoldAnsi.PINK);
        StringBuilder result = new StringBuilder();
        int colorIndex = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\n') {
                result.append(rainbow.get(colorIndex));
                colorIndex = (colorIndex + 1) % rainbow.size();
            } else {
                if (c == '\n') {
                    colorIndex = 0;
                }
            }
            result.append(c);
        }
        result.append(RegularAnsi.RESET);
        return result.toString();
    }


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
            return RegularAnsi.PINK + NAME + RegularAnsi.RESET;
        }
        return NAME;
    }

    private static void printInitialization(TardigradeConfiguration config)  {

        var actual_banner = detectUTF8() ? BANNER : NON_UTF_BANNER;
        say(config.color() ? (RegularAnsi.PINK + actual_banner + RegularAnsi.RESET) : actual_banner);
        say(INIT_MESSAGE);
        say((config.color() ? fullColor : monochrome) + " logging enabled.");
        var input = (config.color() ? RegularAnsi.DARK_GREEN : "") + "/" + config.input() + RegularAnsi.RESET;
        var output = (config.color() ? RegularAnsi.DARK_RED : "") + "/" + config.output() + RegularAnsi.RESET;
        say("Reading files from: " + input + ", writing files to: " + output);
        say("Attempting to bind to port " + config.port());
    }

    private static boolean detectUTF8() {
        var console = System.console();
        if (console == null) return true;
        say("Console detected with charset = " + console.charset());
        return console.charset().equals(StandardCharsets.UTF_8);
    }


    public static void say(Object o) {
        if (!config.quiet()) {
            System.out.println(o);
        }
    }
}