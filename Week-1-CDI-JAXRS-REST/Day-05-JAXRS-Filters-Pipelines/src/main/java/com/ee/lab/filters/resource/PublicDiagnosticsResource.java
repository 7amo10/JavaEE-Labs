package com.ee.lab.filters.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

@Path("/diagnostics")
@Produces(MediaType.APPLICATION_JSON)
public class PublicDiagnosticsResource {

    @GET
    @Path("/ping")
    public Response ping() {
        System.out.println("  [RESOURCE EXECUTION] Invoking PublicDiagnosticsResource.ping()");
        Map<String, Object> statusPayload = Map.of(
            "status", "UP",
            "server_time", Instant.now().toString(),
            "jvm_uptime_ms", ManagementFactory.getRuntimeMXBean().getUptime(),
            "access_tier", "PUBLIC"
        );

        return Response.ok(statusPayload).build();
    }
}
