package com.example.distributed_systems.service;
import com.example.distributed_systems.config.WorkerConfig;
import com.example.distributed_systems.dto.KVRecord;
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

    public boolean putKey(String key, String value) {
        store.put(key, value); // store locally (primary)
        KVRecord record = new KVRecord(key, value);

        List<String> replicaUrls = workerConfig.getReplicaUrls(workerId);

        int successCount = 1; // self success

        // --- Send to first replica (synchronous) ---
        try {
            restTemplate.put(replicaUrls.get(0) + "/worker/replicate/" + key, record);
            successCount++;
        } catch (Exception e) {
            System.err.println("Replica 1 write failed: " + e.getMessage());
        }

        // --- Send to second replica (asynchronous) ---
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.put(replicaUrls.get(1) + "/worker/replicate/" + key, record);
                System.out.println("Replica 2 written (async)");
            } catch (Exception e) {
                System.err.println("Replica 2 async write failed: " + e.getMessage());
            }
        });

        return successCount >= 2;
    }

    public void storeReplica(String key, KVRecord record) {
        store.put(record.getKey(), record.getValue());
        System.out.println("Replica stored for key=" + key + " at worker=" + workerId);
    }

    public String getKey(String key) {
        return store.get(key);
    }
}
