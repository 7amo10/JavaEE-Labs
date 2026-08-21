package com.ee.lab.pulse.repository;

import com.ee.lab.pulse.model.HealthStatus;
import com.ee.lab.pulse.model.JvmSnapshot;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class TelemetryStore {

    private static final TelemetryStore INSTANCE = new TelemetryStore();
    private final Map<String, JvmSnapshot> store = new ConcurrentHashMap<>();

    public TelemetryStore() {
        // Seed default initial snapshot
        save(new JvmSnapshot("SNAP-INIT-01", "node-cluster-master", HealthStatus.OPTIMAL, 128, 2048, 18, 5.2));
    }

    public static TelemetryStore getInstance() {
        return INSTANCE;
    }

    public Optional<JvmSnapshot> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<JvmSnapshot> findAll() {
        return new ArrayList<>(store.values());
    }

    public JvmSnapshot save(JvmSnapshot snapshot) {
        store.put(snapshot.getSnapshotId(), snapshot);
        return snapshot;
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }

    public int count() {
        return store.size();
    }
}
