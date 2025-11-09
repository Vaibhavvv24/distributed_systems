package com.example.distributed_systems.config;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;



@Component
public class WorkerConfig {

    private final Map<String, List<String>> replicaMap = Map.of(
        "worker-1", List.of("http://localhost:8082", "http://localhost:8083"),
        "worker-2", List.of("http://localhost:8083", "http://localhost:8084"),
        "worker-3", List.of("http://localhost:8084", "http://localhost:8081"),
        "worker-4", List.of("http://localhost:8081", "http://localhost:8082")
    );

    public List<String> getReplicaUrls(String workerId) {
        return replicaMap.getOrDefault(workerId, List.of());
    }
}
