package com.ee.lab.tuning.boundary;

import com.ee.lab.tuning.control.DataSourceManager;
import com.ee.lab.tuning.control.PerformanceTelemetryService;
import com.ee.lab.tuning.entity.GcPauseMetric;
import com.ee.lab.tuning.entity.PoolStatistics;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsThroughputResource {

    private static volatile DataSourceManager activeDataSource;
    private static final PerformanceTelemetryService telemetryService = new PerformanceTelemetryService();
    private static final AtomicLong totalRequestsHandled = new AtomicLong(0);
    private static final AtomicLong totalConnectionTimeouts = new AtomicLong(0);

    public static void setActiveDataSource(DataSourceManager ds) {
        activeDataSource = ds;
    }

    public static void resetCounters() {
        totalRequestsHandled.set(0);
        totalConnectionTimeouts.set(0);
    }

    public static long getTotalRequestsHandled() {
        return totalRequestsHandled.get();
    }

    public static long getTotalConnectionTimeouts() {
        return totalConnectionTimeouts.get();
    }

    @GET
    @Path("/ping")
    public Response ping() {
        return Response.ok(Map.of("status", "UP", "timestamp", System.currentTimeMillis())).build();
    }

    @GET
    @Path("/query")
    public Response executeWorkload(@QueryParam("delay") Long delayMs,
                                   @QueryParam("gcPressure") Integer gcPressure) {
        long reqId = totalRequestsHandled.incrementAndGet();
        long simulatedDelay = (delayMs != null && delayMs > 0) ? delayMs : 5L;

        if (gcPressure != null && gcPressure > 0) {
            telemetryService.triggerSimulatedGcPressure(gcPressure);
        }

        if (activeDataSource == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "DataSource not configured"))
                    .build();
        }

        long start = System.nanoTime();
        try {
            int rowCount = activeDataSource.executeQuery(simulatedDelay);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            Map<String, Object> result = new HashMap<>();
            result.put("requestId", reqId);
            result.put("auditEventsCount", rowCount);
            result.put("executionTimeMs", elapsedMs);
            result.put("poolStats", activeDataSource.getPoolStatistics());

            return Response.ok(result).build();
        } catch (SQLException e) {
            totalConnectionTimeouts.incrementAndGet();
            return Response.status(Response.Status.GATEWAY_TIMEOUT)
                    .entity(Map.of(
                            "error", "Connection Acquisition Failed / Timeout",
                            "message", e.getMessage(),
                            "poolStats", activeDataSource.getPoolStatistics()
                    ))
                    .build();
        }
    }

    @GET
    @Path("/pool-status")
    public Response getPoolStatus() {
        if (activeDataSource == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        PoolStatistics stats = activeDataSource.getPoolStatistics();
        List<GcPauseMetric> gc = telemetryService.captureGcMetrics();
        return Response.ok(Map.of(
                "poolStatistics", stats,
                "gcMetrics", gc,
                "totalRequests", totalRequestsHandled.get(),
                "totalTimeouts", totalConnectionTimeouts.get()
        )).build();
    }
}
