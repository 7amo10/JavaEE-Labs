package com.ee.lab.sse.boundary;

import com.ee.lab.sse.control.ClusterBroadcasterManager;
import com.ee.lab.sse.entity.BroadcastAck;
import com.ee.lab.sse.entity.ClusterAlertEvent;
import com.ee.lab.sse.entity.ClusterTelemetryEvent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/cluster")
public class ClusterStreamResource {

    private final ClusterBroadcasterManager broadcasterManager = ClusterBroadcasterManager.getInstance();

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribeClusterEvents(
            @Context SseEventSink eventSink,
            @Context Sse sse) {

        System.out.println("[BOUNDARY] Client subscribing to cluster broadcast channel.");
        broadcasterManager.registerSink(eventSink, sse);
    }

    @POST
    @Path("/telemetry")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishTelemetry(ClusterTelemetryEvent telemetry, @Context Sse sse) {
        broadcasterManager.broadcastTelemetry(sse, telemetry);
        return Response.accepted(new BroadcastAck("DISPATCHED", broadcasterManager.getActiveSinksCount())).build();
    }

    @POST
    @Path("/alerts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishAlert(ClusterAlertEvent alert, @Context Sse sse) {
        broadcasterManager.broadcastAlert(sse, alert);
        return Response.accepted(new BroadcastAck("ALERT_BROADCASTED", broadcasterManager.getActiveSinksCount())).build();
    }

    @GET
    @Path("/subscribers/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveSubscribers() {
        return Response.ok(new BroadcastAck("ACTIVE", broadcasterManager.getActiveSinksCount())).build();
    }
}
