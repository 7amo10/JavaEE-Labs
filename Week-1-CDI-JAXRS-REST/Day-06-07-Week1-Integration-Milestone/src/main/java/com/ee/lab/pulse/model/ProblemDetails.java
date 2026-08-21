package com.ee.lab.pulse.model;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.Map;

public class ProblemDetails {

    @JsonbProperty("type")
    private String type;

    @JsonbProperty("title")
    private String title;

    @JsonbProperty("status")
    private int status;

    @JsonbProperty("detail")
    private String detail;

    @JsonbProperty("instance")
    private String instance;

    @JsonbProperty("timestamp")
    private String timestamp;

    @JsonbProperty("invalid_params")
    private Map<String, String> invalidParams;

    public ProblemDetails() {
        this.timestamp = Instant.now().toString();
    }

    public ProblemDetails(String type, String title, int status, String detail, String instance) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.timestamp = Instant.now().toString();
    }

    public ProblemDetails(String type, String title, int status, String detail, String instance, Map<String, String> invalidParams) {
        this(type, title, status, detail, instance);
        this.invalidParams = invalidParams;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getInstance() { return instance; }
    public void setInstance(String instance) { this.instance = instance; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getInvalidParams() { return invalidParams; }
    public void setInvalidParams(Map<String, String> invalidParams) { this.invalidParams = invalidParams; }
}
