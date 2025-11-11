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

    // Step 1: Ask controller for mapping (primary + replicas)
    RouteResponse mapping = restTemplate.getForObject(
        CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8),
        RouteResponse.class
    );

    if (mapping == null || mapping.getPrimary() == null || mapping.getReplicas() == null || mapping.getReplicas().isEmpty()) {
        return new ClientPutResponse(false, "No workers available");
    }

    WorkerInfo primary = mapping.getPrimary();
    List<WorkerInfo> replicas = mapping.getReplicas();
    int successCount = 0;

    // ---------- Primary write (synchronous) ----------
    try {
        String primaryUrl = "http://" + primary.getHost() + ":" + primary.getPort() + "/v1/worker/put";
        PutRequest req = new PutRequest();
        req.setKey(key);
        req.setValue(value);

        PutResponse resp = restTemplate.postForObject(primaryUrl, req, PutResponse.class);
        if (resp != null && resp.isSuccess()) {
            successCount++;
            System.out.println("🟢 Written to primary: " + primary.getId());
        }
    } catch (Exception e) {
        System.out.println("❌ PUT failed on primary " + primary.getId() + ": " + e.getMessage());
    }

    // ---------- Replica 1 write (synchronous) ----------
    if (replicas.size() >= 1) {
        WorkerInfo replica1 = replicas.get(0);
        try {
            String url = "http://" + replica1.getHost() + ":" + replica1.getPort() + "/v1/worker/put";
            PutRequest req = new PutRequest();
            req.setKey(key);
            req.setValue(value);

            PutResponse resp = restTemplate.postForObject(url, req, PutResponse.class);
            if (resp != null && resp.isSuccess()) {
                successCount++;
                System.out.println("🟢 Written synchronously to replica1: " + replica1.getId());
            }
        } catch (Exception e) {
            System.out.println("❌ PUT failed on replica1 " + replica1.getId() + ": " + e.getMessage());
        }
    }

    // ---------- Replica 2 write (asynchronous) ----------
    if (replicas.size() >= 2) {
        WorkerInfo replica2 = replicas.get(1);
        CompletableFuture.runAsync(() -> {
            try {
                String url = "http://" + replica2.getHost() + ":" + replica2.getPort() + "/v1/worker/put";
                PutRequest req = new PutRequest();
                req.setKey(key);
                req.setValue(value);
                restTemplate.postForObject(url, req, PutResponse.class);
                System.out.println("🟡 Async replication done on replica2: " + replica2.getId());
            } catch (Exception e) {
                System.out.println("⚠️ Async replication failed on replica2 " + replica2.getId() + ": " + e.getMessage());
            }
        }, Executors.newSingleThreadExecutor());
    }

    // ---------- Result ----------
    if (successCount >= 2) { // Primary + one replica OK
        return new ClientPutResponse(true, "Data written successfully to primary and one replica");
    } else {
        return new ClientPutResponse(false, "Failed to write to enough nodes");
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

    if (mapping == null || mapping.getPrimary() == null) {
        return new ClientGetResponse(key, "No primary available");
    }

    WorkerInfo primary = mapping.getPrimary();
    String url = "http://" + primary.getHost() + ":" + primary.getPort() +
                 "/v1/worker/get?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

    try {
        GetResponse resp = restTemplate.getForObject(url, GetResponse.class);
        if (resp != null) {
            System.out.println("🟢 Read value from primary: " + primary.getId());
            return new ClientGetResponse(key, resp.getValue());
        } else {
            return new ClientGetResponse(key, "Not found");
        }
    } catch (Exception e) {
        System.out.println("❌ Primary read failed on " + primary.getId() + ": " + e.getMessage());
        return new ClientGetResponse(key, "Primary unavailable");
    }
}
}
