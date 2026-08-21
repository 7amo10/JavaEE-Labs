package com.ee.lab.pulse.service;

import com.ee.lab.pulse.model.JvmSnapshot;

public interface TelemetryCollector {
    JvmSnapshot captureSnapshot(String nodeName);
}
