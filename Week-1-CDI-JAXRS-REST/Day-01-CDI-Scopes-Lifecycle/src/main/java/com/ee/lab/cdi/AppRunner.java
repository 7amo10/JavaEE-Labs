package com.ee.lab.cdi;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.jboss.weld.context.bound.BoundRequestContext;

import java.util.HashMap;
import java.util.Map;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("               CDI 4.0 SCOPES & CLIENT PROXYING EXPERIMENT");
        System.out.println("================================================================================");

        SeContainerInitializer initializer = SeContainerInitializer.newInstance();

        try (SeContainer container = initializer.initialize()) {

            // Step 1: Inspect Client Proxy Class Name
            RequestContextService proxy = container.select(RequestContextService.class).get();
            System.out.println("\n--- [ANALYSIS 1] CDI CLIENT PROXY INSPECTION ---");
            System.out.println("Injected Proxy Class Name : " + proxy.getClass().getName());
            System.out.println("Is Proxy a Subclass?     : " + proxy.getClass().getName().contains("ClientProxy"));
            System.out.println("Proxy Instance Hash      : " + System.identityHashCode(proxy));

            // Step 2: Execute Request 1
            System.out.println("\n--- [ANALYSIS 2] SIMULATING REQUEST #1 ---");
            BoundRequestContext requestContext = container.select(BoundRequestContext.class).get();
            Map<String, Object> requestDataMap1 = new HashMap<>();

            try {
                requestContext.associate(requestDataMap1);
                requestContext.activate();

                // Call method on proxy -> Invokes target contextual instance lazily
                proxy.processRequest("REQ-101");
            } finally {
                requestContext.invalidate();
                requestContext.deactivate();
                requestContext.dissociate(requestDataMap1);
            }

            // Step 3: Execute Request 2
            System.out.println("\n--- [ANALYSIS 3] SIMULATING REQUEST #2 ---");
            Map<String, Object> requestDataMap2 = new HashMap<>();

            try {
                requestContext.associate(requestDataMap2);
                requestContext.activate();

                // Call method on SAME proxy reference -> Targets a NEW request-scoped instance!
                proxy.processRequest("REQ-102");
            } finally {
                requestContext.invalidate();
                requestContext.deactivate();
                requestContext.dissociate(requestDataMap2);
            }

            System.out.println("\n================================================================================");
            System.out.println("               CONTAINER SHUTDOWN & CLEANUP PHASE");
            System.out.println("================================================================================");
        }
    }
}
