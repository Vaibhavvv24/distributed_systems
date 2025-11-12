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

    // ControllerService.java (or wherever your controller logic is)
@PostConstruct
public void startHeartbeatChecker() {
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
        long now = System.currentTimeMillis();
        for (WorkerInfo w : workers.values()) {
            if (now - w.getLastHeartbeat() > HEARTBEAT_TIMEOUT_MS) {
                w.setAlive(false);
                System.err.println("❌ Worker marked dead: " + w.getId());
            } else {
                w.setAlive(true);
            }
        }
    }, 5, 5, TimeUnit.SECONDS); // check every 5 seconds
}

// Register worker (called when worker starts up)
public synchronized void registerWorker(String id, String host, int port) {
    if (workers.containsKey(id)) {
        WorkerInfo existing = workers.get(id);
        existing.setHost(host);
        existing.setPort(port);
        existing.setAlive(true);
        existing.setLastHeartbeat(System.currentTimeMillis());
    } else {
        WorkerInfo info = new WorkerInfo(id, host, port, true, System.currentTimeMillis());
        workers.put(id, info);
        workerList.add(info);
    }
    System.out.println("✅ Worker registered/updated: " + id + " (" + host + ":" + port + ")");
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

    // 🔍 Filter only alive workers
    List<WorkerInfo> aliveWorkers = snapshot.stream()
            .filter(WorkerInfo::isAlive)
            .toList();

    if (aliveWorkers.isEmpty()) {
        throw new IllegalStateException("No alive workers available");
    }

    // 1️⃣ Choose primary using hashing
    int hash = Math.abs(key.hashCode());
    int primaryIndex = hash % aliveWorkers.size();
    WorkerInfo primary = aliveWorkers.get(primaryIndex);

    // 2️⃣ Determine replicas dynamically (only from alive ones)
    List<WorkerInfo> replicas = new ArrayList<>();
    List<String> replicaUrls = workerConfig.getReplicaUrls(primary.getId());

    if (replicaUrls != null) {
        for (String url : replicaUrls) {
            for (WorkerInfo w : aliveWorkers) {
                String workerUrl = "http://" + w.getHost() + ":" + w.getPort();
                if (workerUrl.equals(url) && w.isAlive()) {
                    replicas.add(w);
                    break;
                }
            }
        }
    }

    // 3️⃣ If replicas < 2, fill from other alive nodes (for redundancy)
    if (replicas.size() < 2) {
        for (WorkerInfo w : aliveWorkers) {
            if (!w.getId().equals(primary.getId()) && !replicas.contains(w)) {
                replicas.add(w);
            }
            if (replicas.size() >= 2) break;
        }
    }

    System.out.printf("📡 Key '%s' → Primary: %s | Replicas: %s%n",
            key, primary.getId(),
            replicas.stream().map(WorkerInfo::getId).toList());

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
