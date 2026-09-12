package com.ee.lab.audit.entity;

public record AuditItemReport(
        String pillarName,
        String testCase,
        boolean passed,
        String findings,
        String metric
) {}
