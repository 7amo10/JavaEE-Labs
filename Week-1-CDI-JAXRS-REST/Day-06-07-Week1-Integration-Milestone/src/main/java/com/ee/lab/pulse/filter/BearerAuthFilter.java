package com.ee.lab.pulse.filter;

import com.ee.lab.pulse.binding.Authenticated;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class BearerAuthFilter implements ContainerRequestFilter {

    private static final String VALID_ADMIN_TOKEN = "Bearer valid-pulse-admin-token-2026";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString("Authorization");
        String correlationId = (String) requestContext.getProperty("correlationId");

        System.out.println("  [AUTH FILTER] Enforcing security on: " 
            + requestContext.getUriInfo().getPath() + " | Correlation-ID: " + correlationId);

        if (authHeader == null || !VALID_ADMIN_TOKEN.equals(authHeader.trim())) {
            System.out.println("  [AUTH FILTER] Aborting: Missing or invalid admin Bearer token");
            String errorJson = String.format("""
                {
                    "type": "https://pulse.enterprise.com/errors/unauthorized",
                    "title": "Unauthorized Access",
                    "status": 401,
                    "detail": "Valid administrator Bearer token required",
                    "instance": "%s"
                }
                """, requestContext.getUriInfo().getRequestUri().getPath());

            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .type("application/problem+json")
                        .header("WWW-Authenticate", "Bearer realm=\"PulseAdmin\"")
                        .entity(errorJson)
                        .build()
            );
            return;
        }

        requestContext.setProperty("authenticatedUser", "pulse-admin");
        System.out.println("  [AUTH FILTER] Admin access granted to: pulse-admin");
    }
}
