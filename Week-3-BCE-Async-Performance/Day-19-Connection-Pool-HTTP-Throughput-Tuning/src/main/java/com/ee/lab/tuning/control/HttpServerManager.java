package com.ee.lab.tuning.control;

import com.ee.lab.tuning.TuningApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;
import java.util.logging.Logger;

public class HttpServerManager {

    private static final Logger LOGGER = Logger.getLogger(HttpServerManager.class.getName());
    private final int port;
    private final int workerThreads;
    private HttpServer server;

    public HttpServerManager(int port, int workerThreads) {
        this.port = port;
        this.workerThreads = workerThreads;
    }

    public void start() {
        URI baseUri = URI.create("http://localhost:" + port + "/");
        TuningApplication config = new TuningApplication();
        
        // Create server without automatically starting
        this.server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config, false);

        // Customize worker thread pool
        for (NetworkListener listener : server.getListeners()) {
            ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setPoolName("Grizzly-Worker-Pool")
                    .setCorePoolSize(workerThreads)
                    .setMaxPoolSize(workerThreads)
                    .setQueueLimit(2000);
            listener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        }

        try {
            this.server.start();
            LOGGER.info(String.format("Started Grizzly HTTP Server on port %d with %d worker threads", port, workerThreads));
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Grizzly HTTP server", e);
        }
    }

    public void stop() {
        if (server != null && server.isStarted()) {
            server.shutdownNow();
            LOGGER.info("Grizzly HTTP Server stopped.");
        }
    }
}
