package com.ee.lab.jwt.rest;

import com.ee.lab.jwt.security.Secured;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/v1/control")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryControlResource {

    @POST
    @Path("/nodes/{id}/restart")
    @Secured
    @RolesAllowed({"ADMIN"})
    public Response restartClusterNode(@PathParam("id") String nodeId, @Context SecurityContext securityContext) {
        String adminPrincipal = securityContext.getUserPrincipal().getName();
        return Response.ok("{\"status\":\"SUCCESS\", \"action\":\"RESTART_NODE\", \"nodeId\":\"" + nodeId + "\", \"dispatchedBy\":\"" + adminPrincipal + "\"}").build();
    }

    @POST
    @Path("/nodes/{id}/scale")
    @Secured
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public Response scaleNodeWorkerThreads(@PathParam("id") String nodeId, @QueryParam("threads") int threads, @Context SecurityContext securityContext) {
        String operatorPrincipal = securityContext.getUserPrincipal().getName();
        return Response.ok("{\"status\":\"SUCCESS\", \"action\":\"SCALE_THREADS\", \"nodeId\":\"" + nodeId + "\", \"targetThreads\":" + threads + ", \"dispatchedBy\":\"" + operatorPrincipal + "\"}").build();
    }

    @GET
    @Path("/nodes")
    @Secured
    @RolesAllowed({"ADMIN", "OPERATOR", "VIEWER"})
    public Response getClusterNodeMetrics(@Context SecurityContext securityContext) {
        String principal = securityContext.getUserPrincipal().getName();
        return Response.ok("{\"status\":\"SUCCESS\", \"activeNodes\":4, \"clusterHealth\":\"OPTIMAL\", \"requestedBy\":\"" + principal + "\"}").build();
    }

    @GET
    @Path("/health")
    @PermitAll
    public Response getPublicGatewayHealth() {
        return Response.ok("{\"gatewayStatus\":\"HEALTHY\", \"timestamp\":\"" + java.time.Instant.now() + "\"}").build();
    }

    @DELETE
    @Path("/emergency-purge")
    @DenyAll
    public Response emergencyDataPurge() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}
