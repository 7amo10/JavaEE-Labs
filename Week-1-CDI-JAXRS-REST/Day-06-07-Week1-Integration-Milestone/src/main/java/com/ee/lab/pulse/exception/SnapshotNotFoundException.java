package com.ee.lab.pulse.exception;

public class SnapshotNotFoundException extends RuntimeException {
    private final String snapshotId;

    public SnapshotNotFoundException(String snapshotId, String message) {
        super(message);
        this.snapshotId = snapshotId;
    }

    public String getSnapshotId() { return snapshotId; }
}
