package com.ee.lab.pulse.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Monitored
@Priority(2000)
public class PerformanceMonitorInterceptor {

    @AroundInvoke
    public Object profileMethod(InvocationContext ctx) throws Exception {
        long startNanos = System.nanoTime();
        String targetMethod = ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName() + "()";
        
        System.out.println("  [INTERCEPTOR ENTRY] >>> Monitoring: " + targetMethod);
        try {
            return ctx.proceed();
        } finally {
            double durationMs = (System.nanoTime() - startNanos) / 1_000_000.0;
            System.out.println("  [INTERCEPTOR EXIT]  <<< Completed: " + targetMethod + " | Duration: " + String.format("%.3f ms", durationMs));
        }
    }
}
