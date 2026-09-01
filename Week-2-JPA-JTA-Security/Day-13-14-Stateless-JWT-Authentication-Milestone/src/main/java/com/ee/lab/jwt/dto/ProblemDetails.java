package com.ee.lab.jwt.dto;

import java.net.URI;
import java.time.Instant;

public class ProblemDetails {

    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private Instant timestamp;

    public ProblemDetails() {
        this.timestamp = Instant.now();
    }

    public ProblemDetails(URI type, String title, int status, String detail, URI instance) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.timestamp = Instant.now();
    }

    public URI getType() { return type; }
    public void setType(URI type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public URI getInstance() { return instance; }
    public void setInstance(URI instance) { this.instance = instance; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
