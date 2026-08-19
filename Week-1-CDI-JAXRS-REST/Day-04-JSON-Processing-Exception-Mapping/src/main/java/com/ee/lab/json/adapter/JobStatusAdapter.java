package com.ee.lab.json.adapter;

import com.ee.lab.json.model.JobStatus;
import jakarta.json.bind.adapter.JsonbAdapter;

public class JobStatusAdapter implements JsonbAdapter<JobStatus, String> {

    @Override
    public String adaptToJson(JobStatus status) {
        if (status == null) return "UNKNOWN";
        return "STATE_" + status.name();
    }

    @Override
    public JobStatus adaptFromJson(String adapted) {
        if (adapted == null || adapted.isBlank()) {
            return JobStatus.QUEUED;
        }
        String clean = adapted.replace("STATE_", "").trim().toUpperCase();
        try {
            return JobStatus.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return JobStatus.QUEUED;
        }
    }
}
