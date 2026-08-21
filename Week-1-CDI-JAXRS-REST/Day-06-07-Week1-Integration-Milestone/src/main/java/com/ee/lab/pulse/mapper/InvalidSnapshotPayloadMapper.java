package com.ee.lab.pulse.mapper;

import com.ee.lab.pulse.exception.InvalidSnapshotPayloadException;
import com.ee.lab.pulse.model.ProblemDetails;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidSnapshotPayloadMapper implements ExceptionMapper<InvalidSnapshotPayloadException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidSnapshotPayloadException ex) {
        ProblemDetails problem = new ProblemDetails(
            "https://pulse.enterprise.com/errors/invalid-payload",
            "Invalid Snapshot Payload",
            Response.Status.BAD_REQUEST.getStatusCode(),
            ex.getMessage(),
            uriInfo != null ? uriInfo.getRequestUri().toString() : "/unknown",
            ex.getFieldErrors()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type("application/problem+json")
                .entity(problem)
                .build();
    }
}
