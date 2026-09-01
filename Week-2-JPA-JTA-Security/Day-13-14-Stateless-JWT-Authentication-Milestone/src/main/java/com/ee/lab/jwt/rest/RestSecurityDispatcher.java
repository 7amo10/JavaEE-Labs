package com.ee.lab.jwt.rest;

import com.ee.lab.jwt.dto.ProblemDetails;
import com.ee.lab.jwt.security.*;
import com.ee.lab.jwt.service.SecurityAuditService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.lang.reflect.Method;
import java.net.URI;

public class RestSecurityDispatcher {

    private final JwtTokenService jwtTokenService;
    private final SecurityAuditService auditService;
    private final TelemetryControlResource controlResource;

    public RestSecurityDispatcher(JwtTokenService jwtTokenService, SecurityAuditService auditService) {
        this.jwtTokenService = jwtTokenService;
        this.auditService = auditService;
        this.controlResource = new TelemetryControlResource();
    }

    /**
     * Simulates the JAX-RS ContainerRequestFilter pipeline and resource dispatch.
     */
    public Response dispatchRequest(String httpMethod, String path, String authHeader, String clientIp, Object... args) {
        try {
            Method targetMethod = resolveMethod(httpMethod, path);
            boolean isSecured = targetMethod.isAnnotationPresent(Secured.class)
                    || targetMethod.getDeclaringClass().isAnnotationPresent(Secured.class);

            SecurityContext securityContext;

            // 1. Authentication Phase (ContainerRequestFilter @Secured)
            if (isSecured) {
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    auditService.recordAudit("REST_AUTH_REJECT", "ANONYMOUS", clientIp, "UNAUTHORIZED", "Missing Bearer token on " + path);
                    return buildProblemResponse(Response.Status.UNAUTHORIZED, "Unauthorized", "Missing or invalid Authorization header", path);
                }

                String tokenString = authHeader.substring("Bearer ".length()).trim();
                JwtClaims claims;
                try {
                    claims = jwtTokenService.validateAndParseToken(tokenString);
                } catch (InvalidSignatureException e) {
                    auditService.recordAudit("TOKEN_TAMPERED", "TAMPERED", clientIp, "REJECTED", e.getMessage());
                    return buildProblemResponse(Response.Status.UNAUTHORIZED, "Invalid Token Signature", e.getMessage(), path);
                } catch (TokenExpiredException e) {
                    auditService.recordAudit("TOKEN_EXPIRED", "EXPIRED", clientIp, "REJECTED", e.getMessage());
                    return buildProblemResponse(Response.Status.UNAUTHORIZED, "Token Expired", e.getMessage(), path);
                } catch (MalformedTokenException e) {
                    auditService.recordAudit("TOKEN_MALFORMED", "MALFORMED", clientIp, "REJECTED", e.getMessage());
                    return buildProblemResponse(Response.Status.UNAUTHORIZED, "Malformed Token", e.getMessage(), path);
                }

                JwtPrincipal principal = new JwtPrincipal(claims.getSubject(), claims);
                securityContext = new JwtSecurityContext(principal, true);
            } else {
                securityContext = new JwtSecurityContext(null, false);
            }

            // 2. Authorization Phase (@RolesAllowed, @DenyAll, @PermitAll)
            if (targetMethod.isAnnotationPresent(DenyAll.class)) {
                auditService.recordAudit("ACCESS_DENIED", getPrincipalName(securityContext), clientIp, "FORBIDDEN", "Endpoint is @DenyAll");
                return buildProblemResponse(Response.Status.FORBIDDEN, "Forbidden", "Endpoint is permanently blocked", path);
            }

            if (targetMethod.isAnnotationPresent(RolesAllowed.class)) {
                RolesAllowed rolesAllowed = targetMethod.getAnnotation(RolesAllowed.class);
                boolean hasRole = false;
                for (String role : rolesAllowed.value()) {
                    if (securityContext.isUserInRole(role)) {
                        hasRole = true;
                        break;
                    }
                }

                if (!hasRole) {
                    auditService.recordAudit("RBAC_REJECTED", getPrincipalName(securityContext), clientIp, "FORBIDDEN",
                            "Principal lacks required roles: " + java.util.Arrays.toString(rolesAllowed.value()));
                    return buildProblemResponse(Response.Status.FORBIDDEN, "Forbidden", "Insufficient role privileges for " + path, path);
                }
            }

            // 3. Resource Execution
            return invokeMethod(targetMethod, securityContext, args);

        } catch (Exception e) {
            return buildProblemResponse(Response.Status.INTERNAL_SERVER_ERROR, "Internal Error", e.getMessage(), path);
        }
    }

    private String getPrincipalName(SecurityContext sc) {
        return (sc != null && sc.getUserPrincipal() != null) ? sc.getUserPrincipal().getName() : "ANONYMOUS";
    }

    private Method resolveMethod(String httpMethod, String path) throws NoSuchMethodException {
        if (path.contains("/restart")) {
            return TelemetryControlResource.class.getMethod("restartClusterNode", String.class, SecurityContext.class);
        } else if (path.contains("/scale")) {
            return TelemetryControlResource.class.getMethod("scaleNodeWorkerThreads", String.class, int.class, SecurityContext.class);
        } else if (path.equals("/api/v1/control/nodes")) {
            return TelemetryControlResource.class.getMethod("getClusterNodeMetrics", SecurityContext.class);
        } else if (path.equals("/api/v1/control/health")) {
            return TelemetryControlResource.class.getMethod("getPublicGatewayHealth");
        } else if (path.equals("/api/v1/control/emergency-purge")) {
            return TelemetryControlResource.class.getMethod("emergencyDataPurge");
        }
        throw new IllegalArgumentException("Unknown route: " + path);
    }

    private Response invokeMethod(Method method, SecurityContext sc, Object... args) throws Exception {
        if (method.getParameterCount() == 2 && method.getParameterTypes()[1].equals(SecurityContext.class)) {
            return (Response) method.invoke(controlResource, args[0], sc);
        } else if (method.getParameterCount() == 3 && method.getParameterTypes()[2].equals(SecurityContext.class)) {
            return (Response) method.invoke(controlResource, args[0], args[1], sc);
        } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(SecurityContext.class)) {
            return (Response) method.invoke(controlResource, sc);
        } else {
            return (Response) method.invoke(controlResource);
        }
    }

    private Response buildProblemResponse(Response.Status status, String title, String detail, String path) {
        ProblemDetails problem = new ProblemDetails(
                URI.create("urn:problem-type:security:" + status.getStatusCode()),
                title,
                status.getStatusCode(),
                detail,
                URI.create(path)
        );
        return Response.status(status).entity(problem).build();
    }
}
