package com.ee.lab.json.resource;

import com.ee.lab.json.exception.DuplicateResourceException;
import com.ee.lab.json.exception.InvalidPayloadException;
import com.ee.lab.json.exception.ResourceNotFoundException;
import com.ee.lab.json.model.AnalysisJob;
import com.ee.lab.json.repository.AnalysisJobRepository;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalysisJobResource {

    private final AnalysisJobRepository repository = AnalysisJobRepository.getInstance();

    @Context
    private UriInfo uriInfo;

    @POST
    public Response submitJob(AnalysisJob job) {
        if (job == null) {
            throw new InvalidPayloadException("Request body must not be empty");
        }

        Map<String, String> fieldErrors = new HashMap<>();
        if (job.getJobId() == null || job.getJobId().isBlank()) {
            fieldErrors.put("job_id", "job_id is required");
        }
        if (job.getTargetBinary() == null || job.getTargetBinary().isBlank()) {
            fieldErrors.put("target_binary", "target_binary class path is required");
        }

        if (!fieldErrors.isEmpty()) {
            throw new InvalidPayloadException("Payload validation failed for analysis job submission", fieldErrors);
        }

        if (repository.exists(job.getJobId())) {
            throw new DuplicateResourceException(job.getJobId(), "Analysis job with ID '" + job.getJobId() + "' already exists");
        }

        AnalysisJob saved = repository.save(job);
        URI location = uriInfo.getAbsolutePathBuilder().path(saved.getJobId()).build();

        return Response.created(location)
                .entity(saved)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getJobById(@PathParam("id") String id) {
        Optional<AnalysisJob> jobOpt = repository.findById(id);
        if (jobOpt.isEmpty()) {
            throw new ResourceNotFoundException(id, "Analysis job '" + id + "' was not found in the processing registry");
        }

        return Response.ok(jobOpt.get()).build();
    }

    @GET
    @Path("/{id}/ast")
    public Response getJobAstTree(@PathParam("id") String id) {
        Optional<AnalysisJob> jobOpt = repository.findById(id);
        if (jobOpt.isEmpty()) {
            throw new ResourceNotFoundException(id, "Analysis job '" + id + "' was not found in the processing registry");
        }

        AnalysisJob job = jobOpt.get();

        // Construct dynamic JSON-P AST representation on the fly
        JsonObject astTree = Json.createObjectBuilder()
                .add("job_id", job.getJobId())
                .add("target_binary", job.getTargetBinary())
                .add("ast_metadata", Json.createObjectBuilder()
                        .add("class_version", "65.0 (Java 21 LTS)")
                        .add("constant_pool_count", 142)
                        .add("access_flags", Json.createArrayBuilder()
                                .add("ACC_PUBLIC")
                                .add("ACC_FINAL")
                                .add("ACC_SUPER"))
                        .add("instruction_stream", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("offset", 0).add("opcode", "aload_0"))
                                .add(Json.createObjectBuilder().add("offset", 1).add("opcode", "invokespecial").add("target", "java/lang/Object.<init>"))
                                .add(Json.createObjectBuilder().add("offset", 4).add("opcode", "return"))))
                .build();

        return Response.ok(astTree).build();
    }
}
