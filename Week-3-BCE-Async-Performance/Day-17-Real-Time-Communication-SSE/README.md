# Week 3 Day 17: Real-Time Communication via Server-Sent Events (SSE)

This module implements real-time unidirectional streaming from server to client using **Jakarta RESTful Web Services 3.1 (JAX-RS SSE)** (`jakarta.ws.rs.sse.*`). It demonstrates how modern enterprise microservices stream live telemetry, long-running job progress, and broadcast system notifications over standard HTTP persistent connections compliant with the W3C Server-Sent Events specification.

## Core Architectural Concepts

1. **JAX-RS SSE Endpoint (`SseEventSink` & `Sse`)**:
   * Injection of `@Context SseEventSink eventSink` and `@Context Sse sse` into JAX-RS resource methods annotated with `@Produces(MediaType.SERVER_SENT_EVENTS)`.
   * Unicast streaming of structured business events to individual connected HTTP clients.
2. **Event Packaging (`OutboundSseEvent`)**:
   * Fluent creation using `sse.newEventBuilder()` with custom event names (`name()`), sequential identifiers (`id()`), client reconnection advice in milliseconds (`reconnectDelay()`), metadata (`comment()`), and JSON-B data payloads (`data()`, `mediaType()`).
3. **Multicast Broadcasting (`SseBroadcaster`)**:
   * Enterprise publish/subscribe channel using `sse.newBroadcaster()`.
   * Multiple client sinks register onto a single broadcast channel (`broadcaster.register(eventSink)`).
   * Simultaneous fan-out of telemetry snapshots and high-priority cluster alerts across all active subscribers (`broadcaster.broadcast(event)`).
4. **Lifecycle Hooks & Eviction**:
   * Registering `onClose(Consumer<SseEventSink>)` to track client disconnects and cleanly unregister dead sinks.
   * Registering `onError(BiConsumer<SseEventSink, Throwable>)` to isolate pipeline failures and prevent broadcaster stall.
5. **Client-Side Stream Protection & Disconnection Detection**:
   * Continuous inspection of `eventSink.isClosed()` before event dispatch to prevent thread leaks and broken pipe socket errors when clients terminate early.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile the module and execute the integration suite:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Unicast Background Job Progress Streaming)**:
   * Client initiates a persistent SSE connection to `GET /api/jobs/batch-etl-701/progress`.
   * Server offloads execution to an asynchronous worker and streams 5 discrete progress events (`SUBMITTED` 0%, `EXTRACTING` 25%, `TRANSFORMING` 55%, `LOADING` 85%, and `COMPLETED` 100%).
   * Server gracefully closes `SseEventSink`, delivering clean EOF to the client.
2. **Scenario 2 (Multicast Broadcast Channel via `SseBroadcaster`)**:
   * Multiple concurrent subscribers (`CLIENT-ALPHA` and `CLIENT-BETA`) connect to `GET /api/cluster/stream`.
   * Server broadcasts node telemetry and security alerts; both clients receive identical events in real-time.
3. **Scenario 3 (Broadcaster Lifecycle & Client Eviction)**:
   * A subscribed client closes its socket; `SseBroadcaster.onClose()` hook triggers and updates active subscriber counters without interrupting remaining listeners.
4. **Scenario 4 (Early Abort Protection)**:
   * Client disconnects prematurely after receiving the initial handshake; server detects socket closure via `eventSink.isClosed()` and terminates the background task cleanly.
