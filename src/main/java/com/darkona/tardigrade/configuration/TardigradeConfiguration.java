package com.darkona.tardigrade.configuration;

import ch.qos.logback.classic.Logger;
import org.apache.commons.cli.*;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TardigradeConfiguration {

    private static final String CONFIG_FILE = "configuration.yml";

    private static final Logger log = (Logger) LoggerFactory.getLogger("Config");

    private final CommandLine cmd;
    private volatile Map<String, Object> yml;

    private boolean enableColor;
    private boolean enableHeader = true;
    private boolean enableBody = true;

    public boolean headers() { return enableHeader; }

    public boolean body() { return enableBody; }

    public String input() {
        return resolveDirectory("i", "input", "input");
    }

    public String output() {
        return resolveDirectory("o", "output", "output");
    }

    public String port() {
        return resolve("p", "port", "8050");
    }

    public String logLevel() {
        return resolve(null, "loglevel", "info");
    }

    public String params() { return cmd.getOptionValue("a", ""); }

    public boolean quiet() { return cmd.hasOption("q"); }

    /**
     * Configuration precedence: command line argument, then
     * configuration.yml, then the built-in default.
     */
    private String resolve(String option, String ymlKey, String fallback) {
        if (option != null && cmd.hasOption(option)) {
            return cmd.getOptionValue(option);
        }
        Object fromYml = yml.get(ymlKey);
        return fromYml != null ? String.valueOf(fromYml) : fallback;
    }

    public TardigradeConfiguration(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder("p").longOpt("port").desc("Server port.").type(Integer.class).numberOfArgs(1).build());
        options.addOption(Option.builder("o").longOpt("output").desc("Output directory for writing files").numberOfArgs(1).build());
        options.addOption(Option.builder("i").longOpt("input").desc("Input directory for loading files").numberOfArgs(1).build());
        options.addOption(Option.builder("q").longOpt("quiet").desc("Quiet mode, no console output.").build());
        options.addOption(Option.builder("d").longOpt("disable").desc("Disable features.").hasArgs().build());

        options.addOption(Option.builder("c").longOpt("config").desc("Configuration file to read instead of the one next to the jar.").numberOfArgs(1).build());
        options.addOption(Option.builder("h").longOpt("help").desc("Print this help message.").build());
        //options.addOption(Option.builder("l").longOpt("logres").desc("File to respond to log requests, taken from input.").hasArgs().build());

        cmd = new DefaultParser().parse(options, args);
        yml = loadInitialYaml();

        enableColor = !Boolean.FALSE.equals(yml.get("color"));

        if (cmd.hasOption("d")) {
            var kwargs = cmd.getOptionValues("d");
            for (String arg : kwargs) {
                if ("color".equalsIgnoreCase(arg)) {
                    enableColor = false;
                }
                if ("header".equalsIgnoreCase(arg)) {
                    enableHeader = false;
                }
                if ("body".equalsIgnoreCase(arg)) {
                    enableBody = false;
                }
            }
        }
    }

    /**
     * Looks for configuration.yml next to the jar so it can be edited without repackaging;
     * if it is not there, falls back to the one bundled in the classpath.
     */
    private Map<String, Object> loadYaml() {
        Path external = externalConfig();
        if (external != null && Files.isReadable(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                return asMap(new Yaml().load(in));
            } catch (IOException | RuntimeException e) {
                // A broken file must not silently fall back to the bundled defaults: that would
                // wipe every mock over a typo. The caller decides what to keep.
                throw new ConfigurationException("Could not read " + external + ": " + e.getMessage(), e);
            }
        }
        return bundledYaml();
    }

    /**
     * At startup a broken file cannot be answered with "keep the previous configuration",
     * because there is none yet: it is reported and the bundled defaults take over.
     */
    private Map<String, Object> loadInitialYaml() {
        try {
            return loadYaml();
        } catch (ConfigurationException e) {
            log.error(e.getMessage());
            log.warn("Falling back to the bundled configuration.");
            return bundledYaml();
        }
    }

    private Map<String, Object> bundledYaml() {
        try (InputStream in = TardigradeConfiguration.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                return asMap(new Yaml().load(in));
            }
        } catch (IOException | RuntimeException e) {
            log.error("Could not read bundled {}: {}", CONFIG_FILE, e.getMessage());
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object loaded) {
        return loaded instanceof Map ? (Map<String, Object>) loaded : Collections.emptyMap();
    }

    private Path externalConfig() {
        if (cmd.hasOption("c")) {
            return Path.of(cmd.getOptionValue("c")).toAbsolutePath().normalize();
        }
        Path home = installDirectory();
        return home == null ? null : home.resolve(CONFIG_FILE);
    }

    /**
     * Directory the jar lives in. Everything Tardigrade reads and writes hangs from here, so a
     * copy of the jar with its own input/, output/ and configuration.yml works from anywhere,
     * whatever directory it was launched from.
     */
    private Path installDirectory() {
        try {
            File jar = new File(TardigradeConfiguration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = jar.isDirectory() ? jar : jar.getParentFile();
            return dir == null ? null : dir.toPath();
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    /** Relative directories hang from the install directory; absolute ones are left alone. */
    private String resolveDirectory(String option, String ymlKey, String fallback) {
        String value = resolve(option, ymlKey, fallback);
        Path directory = Path.of(value);
        if (directory.isAbsolute()) {
            return value;
        }
        Path home = installDirectory();
        return home == null ? value : home.resolve(directory).normalize().toString();
    }

    public boolean color() {
        return enableColor;
    }

    public void setColor(boolean enable) {
        this.enableColor = enable;
    }

    /** Canned responses declared under the yaml key 'mocks'. */
    public List<MockRoute> mocks() {
        Object raw = yml.get("mocks");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<MockRoute> routes = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                try {
                    routes.add(MockRoute.from(map));
                } catch (IllegalArgumentException e) {
                    log.warn("Ignoring mock: {}", e.getMessage());
                }
            }
        }
        return routes;
    }

    /**
     * Re-reads configuration.yml so mocks can be edited without restarting.
     * Port and color are fixed once the server is up and are not affected.
     */
    public void reload() {
        yml = loadYaml();
    }

    /** The editable configuration file next to the jar, or null when there is none. */
    public Path configFile() {
        return externalConfig();
    }
}
