package com.ee.lab.pulse.resource;

import com.ee.lab.pulse.binding.Authenticated;
import com.ee.lab.pulse.exception.InvalidSnapshotPayloadException;
import com.ee.lab.pulse.exception.SnapshotNotFoundException;
import com.ee.lab.pulse.model.JvmSnapshot;
import com.ee.lab.pulse.qualifier.EngineType;
import com.ee.lab.pulse.qualifier.MetricEngine;
import com.ee.lab.pulse.repository.TelemetryStore;
import com.ee.lab.pulse.service.TelemetryCollector;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/telemetry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryResource {

    private final TelemetryCollector collector;
    private final TelemetryStore store;

    @Context
    private UriInfo uriInfo;

    public TelemetryResource() {
        // Resolve CDI-managed beans via standard CDI.current() container lookup
        this.collector = CDI.current().select(TelemetryCollector.class, new MetricEngineLiteral(EngineType.HIGH_PRECISION)).get();
        this.store = CDI.current().select(TelemetryStore.class).get();
    }

    public TelemetryResource(TelemetryCollector collector, TelemetryStore store) {
        this.collector = collector;
        this.store = store;
    }

    public static class MetricEngineLiteral extends AnnotationLiteral<MetricEngine> implements MetricEngine {
        private final EngineType value;

        public MetricEngineLiteral(EngineType value) {
            this.value = value;
        }

        @Override
        public EngineType value() {
            return value;
        }
    }

    @GET
    @Path("/live")
    public Response getLiveTelemetry() {
        System.out.println("  [RESOURCE] Invoking GET /telemetry/live");
        JvmSnapshot liveSnapshot = collector.captureSnapshot("node-live-01");
        return Response.ok(liveSnapshot).build();
    }

    @POST
    @Path("/snapshots")
    public Response createSnapshot(JvmSnapshot snapshot) {
        System.out.println("  [RESOURCE] Invoking POST /telemetry/snapshots");
        if (snapshot == null) {
            throw new InvalidSnapshotPayloadException("Request body must not be empty");
        }

        Map<String, String> errors = new HashMap<>();
        if (snapshot.getNodeName() == null || snapshot.getNodeName().isBlank()) {
            errors.put("node_name", "node_name is required");
        }

        if (!errors.isEmpty()) {
            throw new InvalidSnapshotPayloadException("Validation failed for snapshot submission", errors);
        }

        if (snapshot.getSnapshotId() == null || snapshot.getSnapshotId().isBlank()) {
            snapshot.setSnapshotId("SNAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        JvmSnapshot saved = store.save(snapshot);
        URI location = uriInfo.getAbsolutePathBuilder().path(saved.getSnapshotId()).build();

        return Response.created(location)
                .entity(saved)
                .build();
    }

    @GET
    @Path("/snapshots/{id}")
    public Response getSnapshotById(@PathParam("id") String id) {
        System.out.println("  [RESOURCE] Invoking GET /telemetry/snapshots/" + id);
        JvmSnapshot snapshot = store.findById(id)
                .orElseThrow(() -> new SnapshotNotFoundException(id, "Telemetry snapshot '" + id + "' not found"));

        return Response.ok(snapshot).build();
    }

    @GET
    @Path("/snapshots/{id}/summary")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response getSnapshotSummary(@PathParam("id") String id, @Context HttpHeaders headers) {
        System.out.println("  [RESOURCE] Invoking Content Negotiation on /telemetry/snapshots/" + id + "/summary");
        JvmSnapshot snapshot = store.findById(id)
                .orElseThrow(() -> new SnapshotNotFoundException(id, "Telemetry snapshot '" + id + "' not found"));

        List<MediaType> acceptableMediaTypes = headers.getAcceptableMediaTypes();
        for (MediaType mt : acceptableMediaTypes) {
            if (mt.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                String plainSummary = String.format(
                    "PULSE-SNAPSHOT-SUMMARY | ID=%s | Node=%s | Status=%s | HeapUsed=%dMB | Threads=%d",
                    snapshot.getSnapshotId(), snapshot.getNodeName(), snapshot.getHealthStatus(),
                    snapshot.getHeapUsedMb(), snapshot.getThreadCount()
                );
                return Response.ok(plainSummary, MediaType.TEXT_PLAIN_TYPE).build();
            }
        }

        return Response.ok(snapshot, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @DELETE
    @Path("/snapshots/{id}")
    public Response deleteSnapshot(@PathParam("id") String id) {
        System.out.println("  [RESOURCE] Invoking DELETE /telemetry/snapshots/" + id);
        if (!store.delete(id)) {
            throw new SnapshotNotFoundException(id, "Cannot delete non-existent snapshot '" + id + "'");
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/diagnostics/memory-map")
    public Response getMemoryMapTree() {
        System.out.println("  [RESOURCE] Invoking GET /telemetry/diagnostics/memory-map (JSON-P Tree)");
        
        var arrayBuilder = Json.createArrayBuilder();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            arrayBuilder.add(Json.createObjectBuilder()
                    .add("pool_name", pool.getName())
                    .add("type", pool.getType().toString())
                    .add("used_mb", pool.getUsage().getUsed() / (1024 * 1024))
                    .add("max_mb", pool.getUsage().getMax() / (1024 * 1024)));
        }

        JsonObject memoryMap = Json.createObjectBuilder()
                .add("subsystem", "JVM Memory Management Subsystem")
                .add("generated_at", Instant.now().toString())
                .add("memory_pools", arrayBuilder)
                .build();

        return Response.ok(memoryMap).build();
    }

    @GET
    @Path("/admin/audit")
    @Authenticated
    public Response getAdminAudit() {
        System.out.println("  [RESOURCE] Invoking Protected Admin Audit");
        Map<String, Object> audit = Map.of(
            "total_snapshots_retained", store.count(),
            "service_uptime_ms", ManagementFactory.getRuntimeMXBean().getUptime(),
            "jvm_spec", System.getProperty("java.vm.name") + " (" + System.getProperty("java.version") + ")",
            "security_tier", "ADMIN_PRIVILEGED"
        );
        return Response.ok(audit).build();
    }
}
