package com.example.distributed_systems.service;
import com.example.distributed_systems.config.WorkerConfig;
import com.example.distributed_systems.dto.KVRecord;
import com.example.distributed_systems.dto.PutResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;




@Service
public class WorkerService {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Value("${worker.id}")
    private String workerId;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WorkerConfig workerConfig;  // contains info about other workers

     public PutResponse put(String key, String value) {
        // 1️⃣ Store locally (primary)
        store.put(key, value);
        KVRecord record = new KVRecord(key, value);

        List<String> replicaUrls = workerConfig.getReplicaUrls(workerId);

        int successCount = 1; // local write counts as success
        StringBuilder log = new StringBuilder();

        // 2️⃣ Send to first replica (synchronous)
        try {
            restTemplate.put(replicaUrls.get(0) + "/v1/worker/replicate/" + key, record);
            successCount++;
            log.append("Replica 1 written successfully. ");
        } catch (Exception e) {
            log.append("Replica 1 write failed: ").append(e.getMessage()).append(". ");
        }

        // 3️⃣ Send to second replica (asynchronous)
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.put(replicaUrls.get(1) + "/v1/worker/replicate/" + key, record);
                System.out.println("Replica 2 written (async)");
            } catch (Exception e) {
                System.err.println("Replica 2 async write failed: " + e.getMessage());
            }
        });

        boolean success = successCount >= 2;
        return new PutResponse(key, success, log.toString());
    }

    public void storeReplica(String key, KVRecord record) {
        store.put(record.getKey(), record.getValue());
        System.out.println("Replica stored for key=" + key + " at worker=" + workerId);
    }

    public String get(String key) {
        return store.get(key);
    }
}
