package com.ee.lab.json.model;

import com.ee.lab.json.adapter.JobStatusAdapter;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnalysisJob {

    @JsonbProperty("job_id")
    private String jobId;

    @JsonbProperty("target_binary")
    private String targetBinary;

    @JsonbProperty("job_status")
    @JsonbTypeAdapter(JobStatusAdapter.class)
    private JobStatus status;

    @JsonbProperty("created_at")
    @JsonbDateFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonbTransient
    private String internalSecuritySignature;

    @JsonbProperty("opcode_count")
    private int opcodeCount;

    @JsonbProperty("cyclomatic_complexity")
    private double cyclomaticComplexity;

    public AnalysisJob() {
        this.createdAt = LocalDateTime.now();
        this.internalSecuritySignature = "SIGNATURE-" + UUID.randomUUID();
    }

    public AnalysisJob(String jobId, String targetBinary, JobStatus status, int opcodeCount, double cyclomaticComplexity) {
        this.jobId = jobId;
        this.targetBinary = targetBinary;
        this.status = status;
        this.opcodeCount = opcodeCount;
        this.cyclomaticComplexity = cyclomaticComplexity;
        this.createdAt = LocalDateTime.now();
        this.internalSecuritySignature = "SIGNATURE-" + UUID.randomUUID();
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getTargetBinary() { return targetBinary; }
    public void setTargetBinary(String targetBinary) { this.targetBinary = targetBinary; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getInternalSecuritySignature() { return internalSecuritySignature; }
    public void setInternalSecuritySignature(String internalSecuritySignature) { this.internalSecuritySignature = internalSecuritySignature; }

    public int getOpcodeCount() { return opcodeCount; }
    public void setOpcodeCount(int opcodeCount) { this.opcodeCount = opcodeCount; }

    public double getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(double cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }
}
