package com.ee.lab.filters.filter;

import com.ee.lab.filters.binding.Authenticated;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class BearerTokenAuthFilter implements ContainerRequestFilter {

    private static final String VALID_TOKEN = "Bearer valid-telemetry-token-2026";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString("Authorization");
        String correlationId = (String) requestContext.getProperty("correlationId");

        System.out.println("  [AUTH FILTER] Intercepting protected resource: " 
            + requestContext.getUriInfo().getPath() + " | Correlation-ID: " + correlationId);

        if (authHeader == null || authHeader.isBlank()) {
            System.out.println("  [AUTH FILTER] Aborting: Missing Authorization header");
            abortWithUnauthorized(requestContext, "Missing Authorization header");
            return;
        }

        if (!VALID_TOKEN.equals(authHeader.trim())) {
            System.out.println("  [AUTH FILTER] Aborting: Invalid Bearer Token");
            abortWithUnauthorized(requestContext, "Invalid or expired Bearer token");
            return;
        }

        // Token is valid: Attach authenticated user principal to request context
        requestContext.setProperty("authenticatedUser", "operator-alice");
        System.out.println("  [AUTH FILTER] Authentication succeeded for user: operator-alice");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String detail) {
        String errorJson = String.format("""
            {
                "type": "https://api.example.com/errors/unauthorized",
                "title": "Unauthorized Access",
                "status": 401,
                "detail": "%s",
                "instance": "%s"
            }
            """, detail, requestContext.getUriInfo().getRequestUri().getPath());

        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                    .type("application/problem+json")
                    .header("WWW-Authenticate", "Bearer realm=\"TelemetryEngine\"")
                    .entity(errorJson)
                    .build()
        );
    }
}
