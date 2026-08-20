package com.darkona.tardigrade;

import com.darkona.tardigrade.configuration.ConfigWatcher;
import com.darkona.tardigrade.configuration.MockRouter;
import com.darkona.tardigrade.configuration.TardigradeConfiguration;
import com.darkona.tardigrade.handlers.LogHandler;
import com.darkona.tardigrade.handlers.MockHandler;
import com.darkona.tardigrade.handlers.ReadHandler;
import com.darkona.tardigrade.handlers.WriteHandler;
import com.darkona.tardigrade.recording.RequestLog;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * The server itself, with a life cycle of its own so it can run inside a test as well as
 * from the command line. Holds no static state: several instances can share one JVM.
 */
public class TardigradeServer {

    /** Seconds given to in-flight exchanges when stopping. */
    private static final int STOP_DELAY = 0;

    private final TardigradeConfiguration config;
    private final RequestLog requests;

    private HttpServer server;
    private MockHandler mockHandler;
    private Runnable afterReload = () -> {
    };

    public TardigradeServer(TardigradeConfiguration config) {
        this(config, new RequestLog());
    }

    public TardigradeServer(TardigradeConfiguration config, RequestLog requests) {
        this.config = config;
        this.requests = requests;
    }

    /**
     * Binds and starts serving.
     *
     * @return the port actually bound, which is the one to use when the configured port is 0
     *         and the operating system picks a free one.
     */
    public int start() throws IOException {
        if (server != null) {
            throw new IllegalStateException("This server is already running on port " + port());
        }
        server = HttpServer.create(new InetSocketAddress(Integer.parseInt(config.port())), 0);
        server.createContext("/read", new ReadHandler(config));
        server.createContext("/write", new WriteHandler(config));
        server.createContext("/log", new LogHandler(config, requests));
        // Catch-all: every other path is logged too, so clients can be pointed here
        // without having to change their URLs.
        MockRouter router = new MockRouter(config.mocks());
        router.report(inputDirectory());
        mockHandler = new MockHandler(config, router, new LogHandler(config, requests));
        server.createContext("/", mockHandler);
        ConfigWatcher.start(config.configFile(), this::reload);
        server.start();
        return port();
    }

    public void stop() {
        if (server != null) {
            server.stop(STOP_DELAY);
            server = null;
            mockHandler = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    /** Port in use, or -1 while the server is stopped. */
    public int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    /** Base URL to point a client at, once started. */
    public String baseUrl() {
        return "http://localhost:" + port();
    }

    /** Requests received so far, to assert on. */
    public RequestLog requests() {
        return requests;
    }

    public TardigradeConfiguration configuration() {
        return config;
    }

    /**
     * Extra work to run after each reload. The command line uses it to refresh the log level,
     * which an embedded server leaves alone: changing the root logger of a host application
     * would be rude.
     */
    public void onReload(Runnable action) {
        this.afterReload = action;
    }

    /** Re-reads the configuration file and swaps the mock table. */
    public void reload() {
        config.reload();
        MockRouter router = new MockRouter(config.mocks());
        router.report(inputDirectory());
        if (mockHandler != null) {
            mockHandler.setRouter(router);
        }
        afterReload.run();
    }

    private Path inputDirectory() {
        return Path.of(config.input()).toAbsolutePath().normalize();
    }
}
