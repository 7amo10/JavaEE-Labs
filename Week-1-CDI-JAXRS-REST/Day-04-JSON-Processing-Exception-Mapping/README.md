# Day 04 Lab: JSON-B / JSON-P Processing and Unified RFC-7807 Exception Mapping

This laboratory exercise explores Jakarta JSON Binding 3.0 (JSON-B), Jakarta JSON Processing 2.1 (JSON-P), and centralized RFC-7807 problem details exception handling in Jakarta EE 10 using JAX-RS 3.1.

## Lab Architecture

The lab implements an enterprise RESTful analysis job processing pipeline under package `com.ee.lab.json`:

1. **Jakarta JSON Binding (JSON-B 3.0)**:
   * `@JsonbProperty`: Renames Java camelCase fields to snake_case API contracts (`job_id`, `target_binary`).
   * `@JsonbDateFormat`: Standardizes date-time string formatting (`yyyy-MM-dd HH:mm:ss`).
   * `@JsonbTransient`: Completely redacts sensitive internal security keys from serialized JSON responses.
   * `@JsonbTypeAdapter`: Maps `JobStatus` enum values to customized string representations using `JobStatusAdapter`.
2. **Jakarta JSON Processing (JSON-P 2.1)**:
   * Dynamically constructs nested Abstract Syntax Tree (AST) JSON structures on-the-fly using `Json.createObjectBuilder()` and `JsonArrayBuilder` without fixed Java POJO definitions.
3. **Unified RFC-7807 Exception Handling (`ExceptionMapper`)**:
   * Standardizes error responses into `application/problem+json` payloads containing `type`, `title`, `status`, `detail`, `instance`, and `invalid_params`.
   * Maps domain exceptions to standard HTTP error codes:
     * `ResourceNotFoundException` -> HTTP 404 (Not Found)
     * `InvalidPayloadException` -> HTTP 400 (Bad Request) with field validation errors
     * `DuplicateResourceException` -> HTTP 409 (Conflict)
     * `Throwable` -> HTTP 500 (Internal Server Error)
4. **Embedded Integration Test Runner (`AppRunner`)**:
   * Initializes an embedded Grizzly HTTP server and executes automated test requests via Java 21 `HttpClient`, verifying both success pathways and error mapping contracts.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the lab:

```bash
mvn clean compile exec:java
```

## Expected Verification Flow

1. **Submit Analysis Job**: `POST /api/v1/jobs` returns `201 Created` with JSON-B formatted payload.
2. **Duplicate Conflict**: `POST /api/v1/jobs` with duplicate ID triggers `DuplicateResourceExceptionMapper` returning `409 Conflict` in RFC-7807 JSON format.
3. **Payload Validation Error**: `POST /api/v1/jobs` with empty JSON triggers `InvalidPayloadExceptionMapper` returning `400 Bad Request` with field error mappings.
4. **Not Found Mapping**: `GET /api/v1/jobs/JOB-9999` triggers `ResourceNotFoundExceptionMapper` returning `404 Not Found`.
5. **JSON-B Inspection**: `GET /api/v1/jobs/JOB-101` returns `200 OK` and confirms that `@JsonbTransient` stripped the secret key.
6. **Dynamic JSON-P AST**: `GET /api/v1/jobs/JOB-101/ast` returns `200 OK` with dynamically generated AST bytecode instruction tree.
