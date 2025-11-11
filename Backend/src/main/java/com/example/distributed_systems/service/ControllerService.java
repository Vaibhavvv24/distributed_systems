package com.example.distributed_systems.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.distributed_systems.dto.RouteResponse;
import com.example.distributed_systems.dto.WorkerInfo;
import com.example.distributed_systems.config.WorkerConfig;
import jakarta.annotation.PostConstruct;

@Service
public class ControllerService {


    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private WorkerConfig workerConfig; 

    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final List<WorkerInfo> workerList = Collections.synchronizedList(new ArrayList<>());
    private static final int REPLICATION_FACTOR = 3;
    private static final long HEARTBEAT_TIMEOUT_MS = 10000; // 10 seconds

     @PostConstruct
    public void startHeartbeatChecker() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (WorkerInfo w : workers.values()) {
                if (now - w.getLastHeartbeat() > HEARTBEAT_TIMEOUT_MS) {
                    w.setAlive(false);
                    System.err.println("❌ Worker marked dead: " + w.getId());
                }
            }
        }, 5, 5, TimeUnit.SECONDS); // check every 5 seconds
    }

    // Register worker
    public synchronized void registerWorker(String id, String host, int port) {
        if (workers.containsKey(id)) {
            WorkerInfo existing = workers.get(id);
            existing.setHost(host);
            existing.setPort(port);
            existing.setAlive(true);
        } else {
            WorkerInfo info = new WorkerInfo(id, host, port, true,System.currentTimeMillis());
            workers.put(id, info);
            workerList.add(info);
        }
    }

    // Get mapping of a key to primary + replicas
    public RouteResponse getKeyMapping(String key) {
    if (workerList.isEmpty()) {
        throw new IllegalStateException("No workers available");
    }

    // Take a thread-safe snapshot of workers
    List<WorkerInfo> snapshot;
    synchronized (workerList) {
        snapshot = new ArrayList<>(workerList);
    }

    // 1️⃣ Choose primary using hashing
    int hash = Math.abs(key.hashCode());
    int primaryIndex = hash % snapshot.size();
    WorkerInfo primary = snapshot.get(primaryIndex);

    // 2️⃣ Determine replicas from the static map based on primary ID
    List<WorkerInfo> replicas = new ArrayList<>();
    List<String> replicaUrls = workerConfig.getReplicaUrls(primary.getId());
    System.out.println(replicaUrls.get(0));

    if (replicaUrls != null) {
        for (String url : replicaUrls) {
            // find the WorkerInfo object matching the URL
            for (WorkerInfo w : snapshot) {
                String workerUrl = "http://" + w.getHost() + ":" + w.getPort();
                if (workerUrl.equals(url)) {
                    replicas.add(w);
                    break;
                }
            }
        }
    }

    // 3️⃣ Return route info
    return new RouteResponse(key, primary, replicas);
}
    // Handle worker heartbeat
    public void updateHeartbeat(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setAlive(true);
            worker.setLastHeartbeat(System.currentTimeMillis());
        } else {
            System.err.println("⚠️ Heartbeat from unknown worker: " + workerId);
        }
    }

    // List all workers
    public List<WorkerInfo> listWorkers() {
        return new ArrayList<>(workers.values());
    }

    // Trigger re-replication (placeholder)
    public void triggerReReplication() {
        System.out.println("Re-replication triggered (not implemented yet)");
    }
}
