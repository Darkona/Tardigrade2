package com.darkona.tardigrade;

import ch.qos.logback.classic.Level;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.logging.RegularAnsi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.LoggerFactory;

import static com.darkona.tardigrade.logging.Rainbow.rainbowify;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;


/** Command line entry point. The server itself lives in {@link TardigradeServer}. */
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



    public static void main(String[] args) throws Exception {
        config = new TardigradeConfiguration(args);
        applyLogLevel(config.logLevel());
        if (config.color() && !AnsiConsole.isInstalled() && System.console() != null) {
            AnsiConsole.systemInstall();
        }
        printInitialization(config);

        TardigradeServer server = new TardigradeServer(config);
        server.onReload(() -> applyLogLevel(config.logLevel()));
        int port = server.start();

        System.out.println(name() + " Server is running from http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port);
        say("Also: http://localhost:" + port);
    }

    private static void applyLogLevel(String level) {
        var root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(level, Level.INFO));
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
        var input = paint(RegularAnsi.DARK_GREEN, config.input());
        var output = paint(RegularAnsi.DARK_RED, config.output());
        say("Reading files from: " + input + ", writing files to: " + output);
        say("Attempting to bind to port " + ("0".equals(config.port()) ? "any free one" : config.port()));
    }

    private static String paint(RegularAnsi color, String text) {
        return config.color() ? color + text + RegularAnsi.RESET : text;
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