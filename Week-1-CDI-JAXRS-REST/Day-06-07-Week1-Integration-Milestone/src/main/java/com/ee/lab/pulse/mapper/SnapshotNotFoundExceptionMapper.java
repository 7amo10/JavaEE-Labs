package com.ee.lab.pulse.mapper;

import com.ee.lab.pulse.exception.SnapshotNotFoundException;
import com.ee.lab.pulse.model.ProblemDetails;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SnapshotNotFoundExceptionMapper implements ExceptionMapper<SnapshotNotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(SnapshotNotFoundException ex) {
        ProblemDetails problem = new ProblemDetails(
            "https://pulse.enterprise.com/errors/not-found",
            "JVM Snapshot Not Found",
            Response.Status.NOT_FOUND.getStatusCode(),
            ex.getMessage(),
            uriInfo != null ? uriInfo.getRequestUri().toString() : "/unknown"
        );

        return Response.status(Response.Status.NOT_FOUND)
                .type("application/problem+json")
                .entity(problem)
                .build();
    }
}
