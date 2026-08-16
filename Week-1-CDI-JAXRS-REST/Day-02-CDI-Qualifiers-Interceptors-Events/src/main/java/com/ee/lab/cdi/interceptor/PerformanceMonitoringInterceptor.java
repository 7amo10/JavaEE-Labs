package com.ee.lab.cdi.interceptor;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.management.ThreadMXBean;

@Interceptor
@Monitored
@Priority(Interceptor.Priority.APPLICATION)
public class PerformanceMonitoringInterceptor {

    @Inject
    private ThreadMXBean threadMXBean;

    @AroundInvoke
    public Object monitorPerformance(InvocationContext ctx) throws Exception {
        String targetMethod = ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName() + "()";
        long startCpuTime = threadMXBean.isThreadCpuTimeSupported() ? threadMXBean.getCurrentThreadCpuTime() : 0;
        long startNanos = System.nanoTime();

        System.out.println("\n[INTERCEPTOR ENTRY] >>> Intercepting: " + targetMethod 
            + " | Thread: " + Thread.currentThread().getName() + " [ID: " + Thread.currentThread().threadId() + "]");

        try {
            // Proceed with business method execution
            return ctx.proceed();
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            long cpuTimeUsed = threadMXBean.isThreadCpuTimeSupported() ? (threadMXBean.getCurrentThreadCpuTime() - startCpuTime) : 0;
            double durationMs = durationNanos / 1_000_000.0;

            System.out.println("[INTERCEPTOR EXIT]  <<< Completed: " + targetMethod 
                + " | Wall-Clock Duration: " + String.format("%.3f ms", durationMs)
                + " (" + durationNanos + " ns)"
                + " | Thread CPU Time: " + cpuTimeUsed + " ns\n");
        }
    }
}
