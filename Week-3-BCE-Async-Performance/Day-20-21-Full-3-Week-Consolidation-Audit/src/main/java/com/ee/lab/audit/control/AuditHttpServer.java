package com.ee.lab.audit.control;

import com.ee.lab.audit.AuditApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;
import java.util.logging.Logger;

public class AuditHttpServer {

    private static final Logger LOGGER = Logger.getLogger(AuditHttpServer.class.getName());
    private final int port;
    private HttpServer server;

    public AuditHttpServer(int port) {
        this.port = port;
    }

    public void start() {
        URI baseUri = URI.create("http://localhost:" + port + "/");
        AuditApplication app = new AuditApplication();
        try {
            this.server = GrizzlyHttpServerFactory.createHttpServer(baseUri, app);
            LOGGER.info("Audit Grizzly HTTP Server started on port " + port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Audit HTTP Server", e);
        }
    }

    public void stop() {
        if (server != null && server.isStarted()) {
            server.shutdownNow();
            LOGGER.info("Audit Grizzly HTTP Server stopped.");
        }
    }
}
