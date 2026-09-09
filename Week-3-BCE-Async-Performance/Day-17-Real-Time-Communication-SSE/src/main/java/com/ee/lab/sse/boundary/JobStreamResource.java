package com.ee.lab.sse.boundary;

import com.ee.lab.sse.control.JobProgressService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/jobs")
public class JobStreamResource {

    private final JobProgressService jobProgressService = JobProgressService.getInstance();

    @GET
    @Path("/{jobId}/progress")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamJobProgress(
            @PathParam("jobId") String jobId,
            @Context SseEventSink eventSink,
            @Context Sse sse) {

        System.out.printf("[BOUNDARY] Incoming SSE subscription for jobId=%s%n", jobId);
        jobProgressService.streamJobProgress(jobId, eventSink, sse);
    }
}
