package com.ee.lab.filters.resource;

import com.ee.lab.filters.binding.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.Map;

@Path("/metrics")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class ProtectedMetricsResource {

    @Context
    private ContainerRequestContext requestContext;

    @GET
    @Path("/secure")
    public Response getSecureMetrics() {
        String authenticatedUser = (String) requestContext.getProperty("authenticatedUser");
        System.out.println("  [RESOURCE EXECUTION] Invoking ProtectedMetricsResource.getSecureMetrics() for " + authenticatedUser);

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Map<String, Object> metrics = Map.of(
            "authorized_principal", authenticatedUser != null ? authenticatedUser : "UNKNOWN",
            "heap_used_mb", memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024),
            "heap_max_mb", memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024),
            "active_thread_count", threadMXBean.getThreadCount(),
            "peak_thread_count", threadMXBean.getPeakThreadCount(),
            "sample_timestamp", Instant.now().toString()
        );

        return Response.ok(metrics).build();
    }
}
