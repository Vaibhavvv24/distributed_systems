package com.example.distributed_systems.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.distributed_systems.config.WorkerConfig;
import com.example.distributed_systems.dto.RouteResponse;
import com.example.distributed_systems.dto.WorkerInfo;

import jakarta.annotation.PostConstruct;

@Service
public class ControllerService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WorkerConfig workerConfig;

    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final List<WorkerInfo> workerList = Collections.synchronizedList(new ArrayList<>());

    // Global directory of all key→mapping (primary + replicas)
    private final ConcurrentHashMap<String, RouteResponse> keyDirectory = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_TIMEOUT_MS = 10000;
    private static final int REPLICATION_FACTOR = 3;


    @PostConstruct
    public void startHeartbeatChecker() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (WorkerInfo w : workers.values()) {
                if (now - w.getLastHeartbeat() > HEARTBEAT_TIMEOUT_MS && w.isAlive()) {
                    w.setAlive(false);
                    System.err.println("❌ Worker marked dead: " + w.getId());
                    triggerReReplication(w.getId());
                } else if (now - w.getLastHeartbeat() <= HEARTBEAT_TIMEOUT_MS) {
                    w.setAlive(true);
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }


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


    public RouteResponse getKeyMapping(String key) {
        if (workerList.isEmpty())
            throw new IllegalStateException("No workers available");

        List<WorkerInfo> aliveWorkers = workerList.stream()
                .filter(WorkerInfo::isAlive)
                .toList();

        int hash = Math.abs(key.hashCode());
        WorkerInfo primary = aliveWorkers.get(hash % aliveWorkers.size());

        List<WorkerInfo> replicas = new ArrayList<>();
        for (WorkerInfo w : aliveWorkers) {
            if (!w.getId().equals(primary.getId()))
                replicas.add(w);
            if (replicas.size() >= REPLICATION_FACTOR - 1)
                break;
        }

        RouteResponse route = new RouteResponse(key, primary, replicas);
        keyDirectory.put(key, route);
        return route;
    }

    
    public void updateHeartbeat(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setAlive(true);
            worker.setLastHeartbeat(System.currentTimeMillis());
        }
    }


    public List<WorkerInfo> listWorkers() {
        return new ArrayList<>(workers.values());
    }

    public synchronized void triggerReReplication(String deadWorkerId) {
        System.out.println("🚨 Triggering re-replication for dead worker: " + deadWorkerId);

        List<WorkerInfo> aliveWorkers = workers.values().stream()
                .filter(WorkerInfo::isAlive)
                .collect(Collectors.toList());
        if (aliveWorkers.isEmpty()) {
            System.err.println("⚠️ No alive workers available for re-replication.");
            return;
        }

        for (Map.Entry<String, RouteResponse> entry : keyDirectory.entrySet()) {
            String key = entry.getKey();
            RouteResponse route = entry.getValue();

            boolean affected = route.getPrimary().getId().equals(deadWorkerId)
                    || route.getReplicas().stream().anyMatch(w -> w.getId().equals(deadWorkerId));

            if (!affected) continue;

            // Find a live source that still has the key
            WorkerInfo source = null;
            List<WorkerInfo> currentHolders = new ArrayList<>();
            currentHolders.add(route.getPrimary());
            currentHolders.addAll(route.getReplicas());

            for (WorkerInfo w : currentHolders) {
                if (workers.containsKey(w.getId()) && workers.get(w.getId()).isAlive()) {
                    source = workers.get(w.getId());
                    break;
                }
            }
            if (source == null) {
                System.err.println("⚠️ No alive source found for key: " + key);
                continue;
            }

            // Find a live target that doesn’t have the key
            WorkerInfo target = aliveWorkers.stream()
                    .filter(w -> !currentHolders.contains(w))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                System.err.println("⚠️ No target worker available for key: " + key);
                continue;
            }

            // Fetch value from source and replicate to target
            try {
                String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
              String getUrl = "http://" + source.getHost() + ":" + source.getPort() + "/v1/worker/get?key=" + encodedKey;
                 Map<?, ?> response = restTemplate.getForObject(getUrl, Map.class);
            String value = (response != null) ? (String) response.get("value") : null;


                if (value != null) {
                    restTemplate.put(
                            "http://" + target.getHost() + ":" + target.getPort() + "/v1/worker/replicate/" + encodedKey,
                            Map.of("key", key, "value", value));

                    System.out.printf("🔁 Transferred key '%s' from %s → %s%n",
                            key, source.getId(), target.getId());
                }

                // Update mapping: replace dead worker with target
                WorkerInfo newPrimary = route.getPrimary();
                List<WorkerInfo> newReplicas = new ArrayList<>(route.getReplicas());
                if (route.getPrimary().getId().equals(deadWorkerId)) {
                    newPrimary = target;
                } else {
                    newReplicas.removeIf(w -> w.getId().equals(deadWorkerId));
                    if (!newReplicas.contains(target))
                        newReplicas.add(target);
                }
                keyDirectory.put(key, new RouteResponse(key, newPrimary, newReplicas));

            } catch (Exception e) {
                System.err.println("⚠️ Failed to transfer key " + key + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Re-replication complete for dead worker: " + deadWorkerId);
    }
}
