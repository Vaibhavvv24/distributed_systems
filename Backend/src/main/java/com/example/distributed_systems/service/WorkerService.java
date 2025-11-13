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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.RestTemplate;
import com.example.distributed_systems.dto.RouteResponse;
import com.example.distributed_systems.dto.WorkerInfo;



@Service
public class WorkerService {

    private final Map<String, String> store = new ConcurrentHashMap<>();

      private final String CONTROLLER_URL = "http://localhost:8085/v1/controller/key-mapping";
        private final String CONTROLLER_URL1 = "http://localhost:8085/v1/controller";
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
    int successCount = 1; // local write counts
    StringBuilder log = new StringBuilder();

    try {
        // 2️⃣ Ask controller for alive routing info (dynamic, not static config)
        String controllerUrl = CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8);
        RouteResponse route = restTemplate.getForObject(controllerUrl, RouteResponse.class);

        if (route == null || route.getReplicas() == null || route.getReplicas().isEmpty()) {
            log.append("No alive replicas available. ");
            return new PutResponse(key, successCount >= 2, log.toString());
        }

        List<WorkerInfo> aliveReplicas = route.getReplicas().stream()
                .filter(WorkerInfo::isAlive)
                .toList();

        // ✅ Replica 1 — synchronous
        if (aliveReplicas.size() >= 1) {
            WorkerInfo replica1 = aliveReplicas.get(0);
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
            String replica1Url = "http://" + replica1.getHost() + ":" + replica1.getPort()
                    + "/v1/worker/replicate/" + encodedKey;

            try {
                restTemplate.put(replica1Url, record);
                successCount++;
                log.append("Replica1 written successfully to ")
                        .append(replica1.getId())
                        .append(". ");
            } catch (Exception e) {
                log.append("Replica1 write failed (")
                        .append(replica1.getId())
                        .append("): ")
                        .append(e.getMessage())
                        .append(". ");
            }
        } else {
            log.append("No replica1 available. ");
        }

        // ✅ Replica 2 — asynchronous (after 5 s delay)
        // if (aliveReplicas.size() >= 2) {
        //     WorkerInfo replica2 = aliveReplicas.get(1);
        //     String encodedKey2 = URLEncoder.encode(key, StandardCharsets.UTF_8);
        //     String replica2Url = "http://" + replica2.getHost() + ":" + replica2.getPort()
        //             + "/v1/worker/replicate/" + encodedKey2;

        //     CompletableFuture.runAsync(() -> {
        //         try {
        //             restTemplate.put(replica2Url, record);
        //             System.out.println("🟡 Replica2 written (async after 5 s) → " + replica2.getId());
        //         } catch (Exception e) {
        //             System.err.println("⚠️ Replica2 async write failed (" + replica2.getId() + "): " + e.getMessage());
        //         }
        //     }, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
        // } else {
        //     log.append("No replica2 available. ");
        // }

        if (aliveReplicas.size() >= 2) {
    WorkerInfo replica2 = aliveReplicas.get(1);
    String encodedKey2 = URLEncoder.encode(key, StandardCharsets.UTF_8);
    String replica2Url = "http://" + replica2.getHost() + ":" + replica2.getPort()
            + "/v1/worker/replicate/" + encodedKey2;

    CompletableFuture.runAsync(() -> {
        try {
            restTemplate.put(replica2Url, record);
            System.out.println("🟡 Replica2 written (async after 5 s) → " + replica2.getId());

            // ✅ Notify controller
            String ackUrl = CONTROLLER_URL1 + "/replica/ack?key=" + encodedKey2 + "&replicaId=" + replica2.getId();
            restTemplate.postForObject(ackUrl, null, String.class);

        } catch (Exception e) {
            System.err.println("⚠️ Replica2 async write failed (" + replica2.getId() + "): " + e.getMessage());
        }
    }, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
} else {
    log.append("No replica2 available. ");
}

    } catch (Exception e) {
        log.append("Controller lookup failed: ").append(e.getMessage()).append(". ");
    }

    boolean success = successCount >= 2; // local + at least one replica ok
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
