package com.darkona.tardigrade.configuration;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resolves which canned response matches a request.
 * An exact match always wins; among wildcards, the longest prefix wins.
 */
public class MockRouter {

    private static final Logger log = (Logger) LoggerFactory.getLogger("Mocks");

    private final List<MockRoute> exact = new ArrayList<>();
    private final List<MockRoute> wildcard = new ArrayList<>();

    public MockRouter(List<MockRoute> routes) {
        for (MockRoute route : routes) {
            if (route.isWildcard()) {
                wildcard.add(route);
            } else {
                exact.add(route);
            }
        }
        wildcard.sort(Comparator.comparingInt((MockRoute r) -> r.prefix().length()).reversed());
    }

    public Optional<MockRoute> match(String path, String method) {
        for (MockRoute route : exact) {
            if (route.path().equals(path) && route.acceptsMethod(method)) {
                return Optional.of(route);
            }
        }
        for (MockRoute route : wildcard) {
            if (path.startsWith(route.prefix()) && route.acceptsMethod(method)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    public int size() {
        return exact.size() + wildcard.size();
    }

    /** Warns at startup about mocks pointing at files that do not exist. */
    public void report(Path inputDirectory) {
        if (size() == 0) {
            return;
        }
        log.info("Loaded {} mock route(s).", size());
        for (MockRoute route : all()) {
            if (route.file() != null && !Files.isReadable(inputDirectory.resolve(route.file()))) {
                log.warn("Mock {} points to a missing file: {}", route.path(), inputDirectory.resolve(route.file()));
            }
        }
    }

    public List<MockRoute> all() {
        List<MockRoute> all = new ArrayList<>(exact);
        all.addAll(wildcard);
        return all;
    }
}
