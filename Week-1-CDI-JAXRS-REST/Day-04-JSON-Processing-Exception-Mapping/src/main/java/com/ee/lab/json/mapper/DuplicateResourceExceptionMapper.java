package com.ee.lab.json.mapper;

import com.ee.lab.json.exception.DuplicateResourceException;
import com.ee.lab.json.model.ProblemDetails;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DuplicateResourceExceptionMapper implements ExceptionMapper<DuplicateResourceException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(DuplicateResourceException ex) {
        ProblemDetails problem = new ProblemDetails(
            "https://api.example.com/errors/conflict",
            "Resource Conflict",
            Response.Status.CONFLICT.getStatusCode(),
            ex.getMessage(),
            uriInfo != null ? uriInfo.getRequestUri().toString() : "/unknown"
        );

        return Response.status(Response.Status.CONFLICT)
                .type("application/problem+json")
                .entity(problem)
                .build();
    }
}
