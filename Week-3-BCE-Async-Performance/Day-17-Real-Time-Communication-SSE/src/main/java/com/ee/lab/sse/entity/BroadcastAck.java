package com.ee.lab.sse.entity;

public record BroadcastAck(
    String status,
    int activeSubscribers
) {}
