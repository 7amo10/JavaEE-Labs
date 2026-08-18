package com.ee.lab.jaxrs.repository;

import com.ee.lab.jaxrs.model.TelemetryReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TelemetryRepository {
    private static final TelemetryRepository INSTANCE = new TelemetryRepository();
    private final Map<String, TelemetryReport> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    private TelemetryRepository() {
        // Seed initial mock reports
        save(new TelemetryReport("TEL-101", "node-core-alpha", 42.5, 512, "HEALTHY"));
        save(new TelemetryReport("TEL-102", "node-core-beta", 88.1, 1024, "WARNING"));
        save(new TelemetryReport("TEL-103", "node-edge-gamma", 15.3, 256, "HEALTHY"));
    }

    public static TelemetryRepository getInstance() {
        return INSTANCE;
    }

    public List<TelemetryReport> findAll(String nodeFilter, int limit) {
        return store.values().stream()
            .filter(r -> nodeFilter == null || nodeFilter.isBlank() || r.getNodeId().toLowerCase().contains(nodeFilter.toLowerCase()))
            .limit(limit > 0 ? limit : Long.MAX_VALUE)
            .collect(Collectors.toList());
    }

    public Optional<TelemetryReport> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public TelemetryReport save(TelemetryReport report) {
        if (report.getId() == null || report.getId().isBlank()) {
            report.setId("TEL-" + idGenerator.incrementAndGet());
        }
        store.put(report.getId(), report);
        return report;
    }

    public boolean update(String id, TelemetryReport updated) {
        if (!store.containsKey(id)) {
            return false;
        }
        updated.setId(id);
        store.put(id, updated);
        return true;
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }
}
