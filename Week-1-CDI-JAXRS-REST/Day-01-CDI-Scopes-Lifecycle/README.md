# Day 01 Lab: CDI Scopes, Lifecycle, and Client Proxy Mechanics

This lab demonstrates the internal behavior of normal scopes (`@ApplicationScoped`, `@RequestScoped`) versus pseudo-scopes (`@Dependent`), client proxy generation, and lifecycle callbacks in Jakarta EE 10 CDI 4.0 using Weld SE.

## Lab Architecture

The lab consists of four core components in package `com.ee.lab.cdi`:

1. **`ApplicationCounter` (`@ApplicationScoped`)**: A singleton application-scoped bean that maintains state across request boundaries and tracks cumulative executions.
2. **`RequestContextService` (`@RequestScoped`)**: A normal request-scoped bean instantiated once per active HTTP/request context.
3. **`DependentHelper` (`@Dependent`)**: A pseudo-scoped bean whose lifecycle is bound directly to its parent injection target (`RequestContextService`).
4. **`AppRunner`**: Bootstraps the Weld SE container, programmatically activates request contexts, and logs class names, hash codes, and proxy references.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the lab:

```bash
mvn clean compile exec:java
```

## Expected Output Analysis

When executing `AppRunner`, observe the following runtime behaviors:

1. **Client Proxying**: Printing `proxy.getClass().getName()` yields `com.ee.lab.cdi.RequestContextService$Proxy$_$$_WeldClientProxy`. Injected references for normal-scoped beans hold dynamically generated proxy subclasses rather than raw target instances.
2. **Lazy Target Instantiation**: The target bean constructor and `@PostConstruct` method are not executed when the proxy is created; they execute on demand when the first business method is called within an active context.
3. **Lifecycle Management**: `@PreDestroy` callbacks for request-scoped beans and their injected `@Dependent` helpers execute immediately upon context deactivation, whereas `@ApplicationScoped` beans persist until container shutdown.
