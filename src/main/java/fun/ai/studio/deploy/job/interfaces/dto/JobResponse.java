package fun.ai.studio.deploy.job.interfaces.dto;

import fun.ai.studio.deploy.job.domain.Job;
import fun.ai.studio.deploy.job.domain.JobStatus;
import fun.ai.studio.deploy.job.domain.JobType;
import fun.ai.studio.deploy.runtime.domain.RuntimeNode;

import java.time.Instant;
import java.util.Map;

public class JobResponse {
    private String id;
    private JobType type;
    private JobStatus status;
    private Map<String, Object> payload;
    private String errorMessage;
    private String runnerId;
    private Instant leaseExpireAt;

    /**
     * 仅在 claim 时返回：控制面为本次 job 选择的 runtime 节点（agent/gateway）。
     */
    private RuntimeNode runtimeNode;
    /**
     * 可访问预览地址（由 runtimeNode.gatewayBaseUrl + basePath 计算而来）。
     */
    private String previewUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public static JobResponse from(Job job) {
        JobResponse resp = new JobResponse();
        resp.setId(job.getId().value());
        resp.setType(job.getType());
        resp.setStatus(job.getStatus());
        resp.setPayload(job.getPayload());
        resp.setErrorMessage(job.getErrorMessage());
        resp.setRunnerId(job.getRunnerId());
        resp.setLeaseExpireAt(job.getLeaseExpireAt());
        resp.setCreatedAt(job.getCreatedAt());
        resp.setUpdatedAt(job.getUpdatedAt());
        return resp;
    }

    public static JobResponse from(Job job, RuntimeNode runtimeNode) {
        JobResponse resp = from(job);
        resp.setRuntimeNode(runtimeNode);
        return resp;
    }

    public static JobResponse from(Job job, RuntimeNode runtimeNode, String previewUrl) {
        JobResponse resp = from(job, runtimeNode);
        resp.setPreviewUrl(previewUrl);
        return resp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    public Instant getLeaseExpireAt() {
        return leaseExpireAt;
    }

    public void setLeaseExpireAt(Instant leaseExpireAt) {
        this.leaseExpireAt = leaseExpireAt;
    }

    public RuntimeNode getRuntimeNode() {
        return runtimeNode;
    }

    public void setRuntimeNode(RuntimeNode runtimeNode) {
        this.runtimeNode = runtimeNode;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}


