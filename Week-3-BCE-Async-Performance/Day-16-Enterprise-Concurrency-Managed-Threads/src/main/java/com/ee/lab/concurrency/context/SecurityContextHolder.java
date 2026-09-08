package com.ee.lab.concurrency.context;

/**
 * Simulates Jakarta EE container thread-local context management
 * (such as SecurityContext and transaction/request correlation headers).
 */
public class SecurityContextHolder {

    private static final ThreadLocal<CapturedContext> CONTEXT = new ThreadLocal<>();

    public static void set(String principalName, String correlationId) {
        CONTEXT.set(new CapturedContext(principalName, correlationId));
    }

    public static CapturedContext get() {
        return CONTEXT.get();
    }

    public static String getPrincipalName() {
        CapturedContext ctx = CONTEXT.get();
        return (ctx != null) ? ctx.principalName() : "ANONYMOUS";
    }

    public static String getCorrelationId() {
        CapturedContext ctx = CONTEXT.get();
        return (ctx != null) ? ctx.correlationId() : "N/A";
    }

    public static CapturedContext capture() {
        return CONTEXT.get();
    }

    public static void restore(CapturedContext context) {
        if (context != null) {
            CONTEXT.set(context);
        } else {
            CONTEXT.remove();
        }
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
