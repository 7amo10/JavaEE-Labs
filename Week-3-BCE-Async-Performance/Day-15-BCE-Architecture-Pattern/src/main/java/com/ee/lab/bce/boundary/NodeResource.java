package com.ee.lab.bce.boundary;

import com.ee.lab.bce.control.HealthCalculator;
import com.ee.lab.bce.entity.ClusterNode;
import com.ee.lab.bce.entity.NodeStatus;
import com.ee.lab.bce.entity.NodeSummary;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * JAX-RS REST Boundary stereotype in Adam Bien's BCE pattern.
 * Translates HTTP protocol operations directly into boundary operations.
 * Directly receives and returns JPA Entities and Records, eliminating the "DTO explosion".
 */
@Path("/nodes")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NodeResource {

    @Inject
    private NodeManagementBoundary boundary;

    public NodeResource() {}

    public NodeResource(NodeManagementBoundary boundary) {
        this.boundary = boundary;
    }

    @POST
    public Response createNode(ClusterNode node) {
        ClusterNode created = boundary.registerNode(node);
        return Response.created(URI.create("/nodes/" + created.getId()))
                .entity(created)
                .build();
    }

    @GET
    public Response listAllNodes(@QueryParam("summaryOnly") boolean summaryOnly) {
        if (summaryOnly) {
            List<NodeSummary> summaries = boundary.findNodeSummaries();
            return Response.ok(summaries).build();
        }
        List<ClusterNode> nodes = boundary.findAllNodes();
        return Response.ok(nodes).build();
    }

    @GET
    @Path("/{id}")
    public Response getNode(@PathParam("id") Long id) {
        return boundary.findNode(id)
                .map(node -> Response.ok(node).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id, @QueryParam("status") NodeStatus status) {
        try {
            ClusterNode updated = boundary.transitionStatus(id, status);
            return Response.ok(updated).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"INVALID_STATE_TRANSITION\", \"message\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}/health")
    public Response getHealth(@PathParam("id") Long id) {
        HealthCalculator.HealthAssessment assessment = boundary.assessNodeHealth(id);
        return Response.ok(assessment).build();
    }
}
