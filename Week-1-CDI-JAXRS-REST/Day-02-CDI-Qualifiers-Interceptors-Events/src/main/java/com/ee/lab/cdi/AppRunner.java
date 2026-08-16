package com.ee.lab.cdi;

import com.ee.lab.cdi.service.OrderProcessingService;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   CDI 4.0 QUALIFIERS, PRODUCERS, INTERCEPTORS & EVENTS EXPERIMENT");
        System.out.println("================================================================================");

        SeContainerInitializer initializer = SeContainerInitializer.newInstance();

        try (SeContainer container = initializer.initialize()) {

            System.out.println("\n--- [STEP 1] RETRIEVING MONITORED SERVICE BEAN ---");
            OrderProcessingService orderService = container.select(OrderProcessingService.class).get();

            System.out.println("\n--- [STEP 2] INVOKING INTERCEPTED BUSINESS METHOD ---");
            String result = orderService.processOrder("ORD-1001", 199.95);
            System.out.println(" [APP RUNNER] Order Processing Result: " + result);

            // Brief pause to allow background async event observers to complete
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}

            System.out.println("\n================================================================================");
            System.out.println("               CONTAINER SHUTDOWN & RESOURCE DISPOSAL");
            System.out.println("================================================================================");
        }
    }
}
