package com.ee.lab.audit.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuditSecurityFilter implements ContainerRequestFilter {

    private final SecurityAuditService securityService = new SecurityAuditService();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (path.contains("public") || path.contains("login")) {
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Missing or invalid Authorization header"))
                    .build());
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        String requiredRole = path.contains("admin") ? "ADMIN" : null;

        if (!securityService.validateToken(token, requiredRole)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Access denied: insufficient role privileges or expired token"))
                    .build());
        }
    }
}
