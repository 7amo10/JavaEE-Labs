package com.ee.lab.json.mapper;

import com.ee.lab.json.model.ProblemDetails;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        ProblemDetails problem = new ProblemDetails(
            "https://api.example.com/errors/internal-server-error",
            "Internal Server Error",
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred",
            uriInfo != null ? uriInfo.getRequestUri().toString() : "/unknown"
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type("application/problem+json")
                .entity(problem)
                .build();
    }
}
