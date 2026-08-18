package com.ee.lab.jaxrs.resource;

import com.ee.lab.jaxrs.model.TelemetryReport;
import com.ee.lab.jaxrs.repository.TelemetryRepository;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Path("/telemetry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryResource {

    private final TelemetryRepository repository = TelemetryRepository.getInstance();

    @Context
    private UriInfo uriInfo;

    /**
     * GET /api/v1/telemetry?node={filter}&limit={n}
     * Demonstrates @QueryParam and @DefaultValue.
     */
    @GET
    public Response getAllReports(
            @QueryParam("node") String nodeFilter,
            @QueryParam("limit") @DefaultValue("10") int limit) {
        
        List<TelemetryReport> results = repository.findAll(nodeFilter, limit);
        return Response.ok(results)
                .header("X-Total-Count", results.size())
                .build();
    }

    /**
     * GET /api/v1/telemetry/{id}
     * Demonstrates @PathParam, @HeaderParam, and conditional 200 / 404 responses.
     */
    @GET
    @Path("/{id}")
    public Response getReportById(
            @PathParam("id") String id,
            @HeaderParam("X-Client-Id") String clientId) {
        
        System.out.println("  [SERVER] GET /telemetry/" + id + " requested by Client-Id: " + (clientId != null ? clientId : "ANONYMOUS"));
        
        Optional<TelemetryReport> reportOpt = repository.findById(id);
        if (reportOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Telemetry report not found\", \"id\": \"" + id + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.ok(reportOpt.get()).build();
    }

    /**
     * POST /api/v1/telemetry
     * Demonstrates entity deserialization, validation, 201 Created status, and Location header generation.
     */
    @POST
    public Response createReport(TelemetryReport newReport) {
        if (newReport == null || newReport.getNodeId() == null || newReport.getNodeId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"nodeId is required\"}")
                    .build();
        }

        TelemetryReport saved = repository.save(newReport);
        URI locationUri = uriInfo.getAbsolutePathBuilder().path(saved.getId()).build();

        System.out.println("  [SERVER] Created new TelemetryReport: " + saved.getId() + " at " + locationUri);

        return Response.created(locationUri)
                .entity(saved)
                .build();
    }

    /**
     * PUT /api/v1/telemetry/{id}
     * Demonstrates idempotent resource update.
     */
    @PUT
    @Path("/{id}")
    public Response updateReport(@PathParam("id") String id, TelemetryReport updatedReport) {
        if (updatedReport == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Updated report payload is required\"}")
                    .build();
        }

        boolean updated = repository.update(id, updatedReport);
        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Cannot update non-existent report\", \"id\": \"" + id + "\"}")
                    .build();
        }

        return Response.ok(updatedReport).build();
    }

    /**
     * DELETE /api/v1/telemetry/{id}
     * Demonstrates resource deletion returning 204 No Content.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteReport(@PathParam("id") String id) {
        boolean deleted = repository.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Cannot delete non-existent report\", \"id\": \"" + id + "\"}")
                    .build();
        }

        System.out.println("  [SERVER] Deleted TelemetryReport: " + id);
        return Response.noContent().build();
    }

    /**
     * GET /api/v1/telemetry/{id}/summary
     * Demonstrates Content Negotiation (@Produces with JSON and TEXT_PLAIN).
     */
    @GET
    @Path("/{id}/summary")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response getReportSummary(@PathParam("id") String id) {
        Optional<TelemetryReport> reportOpt = repository.findById(id);
        if (reportOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        TelemetryReport report = reportOpt.get();
        String plainTextSummary = String.format("TELEMETRY-SUMMARY | ID=%s | Node=%s | CPU=%.1f%% | Heap=%dMB | Status=%s",
                report.getId(), report.getNodeId(), report.getCpuLoad(), report.getHeapUsedMb(), report.getStatus());

        return Response.ok(plainTextSummary, MediaType.TEXT_PLAIN_TYPE).build();
    }
}
