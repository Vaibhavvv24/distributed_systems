package com.example.distributed_systems.service;


import com.example.distributed_systems.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class ClientService {

    @Autowired
    private RestTemplate restTemplate;

    private final String CONTROLLER_URL = "http://localhost:8085/v1/controller/key-mapping";

    /**
     * Perform PUT operation:
     * 1. Query controller to get worker mapping.
     * 2. Write synchronously to 2 replicas.
     * 3. Write asynchronously to the 3rd replica.
     */
    public ClientPutResponse put(String key, String value) {
        
      RouteResponse mapping = restTemplate.getForObject(
    CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8),
    RouteResponse.class
);
        if (mapping == null || mapping.getReplicas().isEmpty()) {
            return new ClientPutResponse(false, "No workers available");
        }

        List<WorkerInfo> replicas = mapping.getReplicas();
        int successCount = 0;

        // Write synchronously to 2 replicas
        for (int i = 0; i < Math.min(2, replicas.size()); i++) {
            WorkerInfo worker = replicas.get(i);
            String url = "http://" + worker.getHost() + ":" + worker.getPort() + "/v1/worker/put";
            try {
                PutRequest req = new PutRequest();
                req.setKey(key);
                req.setValue(value);
                PutResponse resp = restTemplate.postForObject(url, req, PutResponse.class);
                if (resp != null && resp.isSuccess()) {
                    successCount++;
                }
            } catch (Exception e) {
                System.out.println("❌ PUT failed on " + worker.getId() + ": " + e.getMessage());
            }
        }

        // Async replication to 3rd worker
        if (replicas.size() > 2) {
            WorkerInfo asyncWorker = replicas.get(2);
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "http://" + asyncWorker.getHost() + ":" + asyncWorker.getPort() + "/v1/worker/put";
                    PutRequest req = new PutRequest();
                    req.setKey(key);
                    req.setValue(value);
                    restTemplate.postForObject(url, req, PutResponse.class);
                    System.out.println("🟡 Async replication done on " + asyncWorker.getId());
                } catch (Exception e) {
                    System.out.println("⚠️ Async replication failed: " + e.getMessage());
                }
            }, Executors.newSingleThreadExecutor());
        }

        if (successCount >= 2) {
            return new ClientPutResponse(true, "Data written successfully to 2 replicas");
        } else {
            return new ClientPutResponse(false, "Failed to write to enough replicas");
        }
    }

    /**
     * Perform GET operation:
     * 1. Query controller to get mapping.
     * 2. Read from primary replica.
     */
    public ClientGetResponse get(String key) {
        RouteResponse mapping = restTemplate.getForObject(
    CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8),
    RouteResponse.class
);
        if (mapping == null || mapping.getReplicas().isEmpty()) {
            return new ClientGetResponse(key, "No replicas available");
        }

        WorkerInfo primary = mapping.getReplicas().get(0);
        String url = "http://" + primary.getHost() + ":" + primary.getPort() + "/v1/worker/get/" + URLEncoder.encode(key, StandardCharsets.UTF_8);

        try {
            GetResponse resp = restTemplate.getForObject(url, GetResponse.class);
            if (resp != null) {
                return new ClientGetResponse(key, resp.getValue());
            } else {
                return new ClientGetResponse(key, "Not found");
            }
        } catch (Exception e) {
            return new ClientGetResponse(key, "Primary unavailable");
        }
    }
}
