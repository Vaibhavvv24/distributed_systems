package com.example.distributed_systems.dto;
import java.util.List;


public class RouteResponse {
    private String key;
    private WorkerInfo primary;
    private List<WorkerInfo> replicas;

    public RouteResponse(String key, WorkerInfo primary, List<WorkerInfo> replicas) {
        this.key = key;
        this.primary = primary;
        this.replicas = replicas;
    }

    public String getKey() {
        return key;
    }

    public WorkerInfo getPrimary() {
        return primary;
    }

    public List<WorkerInfo> getReplicas() {
        return replicas;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPrimary(WorkerInfo primary) {
        this.primary = primary;
    }

    public void setReplicas(List<WorkerInfo> replicas) {
        this.replicas = replicas;
    }
}
