package com.example.distributed_systems.service;
import com.example.distributed_systems.config.WorkerConfig;
import com.example.distributed_systems.dto.KVRecord;
import com.example.distributed_systems.dto.PutResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    // imports used:
// import java.net.URLEncoder;
// import java.nio.charset.StandardCharsets;
// import java.util.List;
// import java.util.concurrent.CompletableFuture;

public PutResponse put(String key, String value) {
    // 1️⃣ Store locally (primary)
    store.put(key, value);
    KVRecord record = new KVRecord(key, value);

    List<String> replicaUrls = workerConfig.getReplicaUrls(workerId); // expected size == 2
    int successCount = 1; // local write counts as success
    StringBuilder log = new StringBuilder();

    // Defensive checks
    if (replicaUrls == null || replicaUrls.isEmpty()) {
        log.append("No replica URLs configured. ");
        boolean success = successCount >= 2;
        return new PutResponse(key, success, log.toString());
    }

    // Ensure we have two replicas in the list (or handle gracefully)
    // Replica 1 (synchronous)
    if (replicaUrls.size() >= 1) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String replica1Url = replicaUrls.get(0) + "/v1/worker/replicate/" + encodedKey;
        try {
            // Using PUT (as your code did). This is a blocking call.
            restTemplate.put(replica1Url, record);
            successCount++;
            log.append("Replica1 written successfully. ");
        } catch (Exception e) {
            log.append("Replica1 write failed: ").append(e.getMessage()).append(". ");
        }
    } else {
        log.append("Replica1 missing. ");
    }

    // Replica 2 (asynchronous)
    if (replicaUrls.size() >= 2) {
        String encodedKey2 = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String replica2Url = replicaUrls.get(1) + "/v1/worker/replicate/" + encodedKey2;

        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.put(replica2Url, record);
                System.out.println("🟡 Replica2 written (async)");
            } catch (Exception e) {
                System.err.println("⚠️ Replica2 async write failed: " + e.getMessage());
            }
        });
    } else {
        log.append("Replica2 missing. ");
    }

    boolean success = successCount >= 2; // local + at least one replica
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
