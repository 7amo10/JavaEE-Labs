package com.ee.lab.json.mapper;

import com.ee.lab.json.exception.ResourceNotFoundException;
import com.ee.lab.json.model.ProblemDetails;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ResourceNotFoundException ex) {
        ProblemDetails problem = new ProblemDetails(
            "https://api.example.com/errors/not-found",
            "Resource Not Found",
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
