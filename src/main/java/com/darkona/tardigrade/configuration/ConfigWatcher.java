package com.darkona.tardigrade.configuration;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Watches configuration.yml and runs the given action whenever it changes,
 * so mocks can be edited while the server is running.
 */
public class ConfigWatcher implements Runnable {

    /** Editors write in several steps; this lets the file settle before reading it. */
    private static final long SETTLE_MILLIS = 200;

    private static final Logger log = (Logger) LoggerFactory.getLogger("Config");

    private final Path file;
    private final Runnable onChange;

    private ConfigWatcher(Path file, Runnable onChange) {
        this.file = file;
        this.onChange = onChange;
    }

    public static void start(Path file, Runnable onChange) {
        if (file == null || !Files.isReadable(file)) {
            log.info("No editable configuration file found, live reload is off.");
            return;
        }
        Thread thread = new Thread(new ConfigWatcher(file, onChange), "config-watcher");
        thread.setDaemon(true);
        thread.start();
        log.info("Watching {} for changes.", file);
    }

    @Override
    public void run() {
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            file.getParent().register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = service.take();
                boolean touched = key.pollEvents().stream()
                        .anyMatch(event -> file.getFileName().toString().equals(String.valueOf(event.context())));
                key.reset();
                if (touched) {
                    Thread.sleep(SETTLE_MILLIS);
                    reload();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Stopped watching the configuration file", e);
        }
    }

    private void reload() {
        try {
            onChange.run();
        } catch (RuntimeException e) {
            // A broken yaml must not take the watcher down: keep the previous state and wait
            // for the next edit.
            log.error("Could not reload the configuration, keeping the previous one", e);
        }
    }
}
