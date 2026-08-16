package com.ee.lab.cdi.producers;

public class TelemetryChannel {
    private final String channelId;
    private boolean active;

    public TelemetryChannel(String channelId) {
        this.channelId = channelId;
        this.active = true;
        System.out.println("  [TelemetryChannel] Allocated new channel: " + channelId + " | Hash: " + System.identityHashCode(this));
    }

    public void transmit(String payload) {
        if (!active) {
            throw new IllegalStateException("Cannot transmit on closed channel: " + channelId);
        }
        System.out.println("  [TelemetryChannel] Transmitting via " + channelId + " -> " + payload);
    }

    public void close() {
        this.active = false;
        System.out.println("  [TelemetryChannel] Closed channel: " + channelId + " | Hash: " + System.identityHashCode(this));
    }

    public String getChannelId() {
        return channelId;
    }
}
