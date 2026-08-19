package com.ee.lab.json.mapper;

import com.ee.lab.json.exception.InvalidPayloadException;
import com.ee.lab.json.model.ProblemDetails;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidPayloadExceptionMapper implements ExceptionMapper<InvalidPayloadException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidPayloadException ex) {
        ProblemDetails problem = new ProblemDetails(
            "https://api.example.com/errors/invalid-payload",
            "Bad Request - Validation Failed",
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
