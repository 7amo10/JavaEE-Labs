package com.ee.lab.audit.control;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.concurrent.atomic.AtomicLong;

public class AuditStatementInspector implements StatementInspector {

    private static final AtomicLong statementCounter = new AtomicLong(0);

    public static void reset() {
        statementCounter.set(0);
    }

    public static long getCount() {
        return statementCounter.get();
    }

    @Override
    public String inspect(String sql) {
        statementCounter.incrementAndGet();
        return sql;
    }
}
