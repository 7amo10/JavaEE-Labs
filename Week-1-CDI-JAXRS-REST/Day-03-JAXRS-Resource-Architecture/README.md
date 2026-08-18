# Day 03 Lab: JAX-RS 3.1 REST Resource Architecture

This laboratory exercise explores Jakarta RESTful Web Services 3.1 (JAX-RS) resource modeling, HTTP verb mapping, parameter extraction, content negotiation, and response status code semantics.

## Lab Architecture

The lab implements a production-grade RESTful API for telemetry report management under package `com.ee.lab.jaxrs`:

1. **Path Routing and Method Mapping (`@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`)**: Standardized HTTP CRUD operations mapped to Java methods.
2. **Parameter Extraction Annotations**:
   * `@PathParam`: Extracts dynamic path segments (`/telemetry/{id}`).
   * `@QueryParam` and `@DefaultValue`: Filters collection queries with default pagination parameters (`/telemetry?limit=5`).
   * `@HeaderParam`: Inspects inbound custom HTTP headers (`X-Client-Id`).
3. **Response Builder Pattern (`Response.created()`, `Response.ok()`, `Response.noContent()`)**: Constructing explicit HTTP status codes (200 OK, 201 Created with `Location` header, 204 No Content, 400 Bad Request, 404 Not Found).
4. **Content Negotiation (`@Produces`, `@Consumes`)**: Dynamically serving either `application/json` or `text/plain` representations based on client `Accept` headers.
5. **Embedded Integration Test Runner (`AppRunner`)**: Initializes an embedded Grizzly HTTP server running Jersey 3.1 and executes end-to-end HTTP client interactions via Java 21 `HttpClient`.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the lab:

```bash
mvn clean compile exec:java
```

## Expected Verification Flow

1. **List Resources**: `GET /api/v1/telemetry?limit=5` returns `200 OK` with JSON array and `X-Total-Count` header.
2. **Create Resource**: `POST /api/v1/telemetry` returns `201 Created` with a `Location: http://localhost:8080/api/v1/telemetry/TEL-101` header.
3. **Inspect Resource**: `GET /api/v1/telemetry/TEL-101` with `X-Client-Id` returns `200 OK`.
4. **Update Resource**: `PUT /api/v1/telemetry/TEL-101` returns `200 OK` with updated telemetry state.
5. **Content Negotiation**: `GET /api/v1/telemetry/TEL-101/summary` with `Accept: text/plain` returns formatted ASCII summary.
6. **Delete Resource**: `DELETE /api/v1/telemetry/TEL-101` returns `204 No Content`.
7. **Verify Deletion**: `GET /api/v1/telemetry/TEL-101` confirms resource removal with `404 Not Found`.
