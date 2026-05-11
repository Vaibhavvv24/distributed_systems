# Distributed Key-Value Store — Interview Preparation Report

> A comprehensive deep-dive into the design, implementation, and distributed systems concepts behind this fault-tolerant, replicated key-value store.

---

## Table of Contents

1. [Project Overview & Motivation](#1-project-overview--motivation)
2. [System Architecture](#2-system-architecture)
3. [Core Distributed Systems Concepts](#3-core-distributed-systems-concepts)
4. [Detailed Component Design](#4-detailed-component-design)
5. [Data Flow — PUT Operation](#5-data-flow--put-operation)
6. [Data Flow — GET Operation](#6-data-flow--get-operation)
7. [Replication Strategy](#7-replication-strategy)
8. [Failure Detection & Heartbeats](#8-failure-detection--heartbeats)
9. [Re-Replication & Self-Healing](#9-re-replication--self-healing)
10. [Concurrency & Thread Safety](#10-concurrency--thread-safety)
11. [API Design & REST Conventions](#11-api-design--rest-conventions)
12. [Frontend Dashboard](#12-frontend-dashboard)
13. [Trade-offs, Limitations & Interview Talking Points](#13-trade-offs-limitations--interview-talking-points)
14. [Potential Interview Questions & Answers](#14-potential-interview-questions--answers)
15. [CAP Theorem Analysis](#15-cap-theorem-analysis)

---

## 1. Project Overview & Motivation

### What is this project?

A **distributed in-memory key-value store** that distributes data across multiple worker nodes with automatic replication and fault tolerance. Think of it as a simplified version of systems like **Amazon DynamoDB**, **Apache Cassandra**, or **Redis Cluster**.

### Problem Statement

Design and implement a distributed storage system that:
- Partitions data across multiple nodes
- Replicates each key to 3 nodes for durability
- Detects node failures automatically
- Self-heals by re-replicating data when a node dies
- Provides a REST API for clients to read/write data

### Technology Choices

| Choice | Rationale |
|--------|-----------|
| **Spring Boot 3.5** | Production-grade Java framework with built-in scheduling, dependency injection, REST support |
| **Java 17** | Modern Java with records, sealed classes, pattern matching |
| **ConcurrentHashMap** | Thread-safe in-memory storage without external DB dependency |
| **RestTemplate** | Synchronous HTTP client for inter-service communication |
| **CompletableFuture** | Non-blocking async replication without dedicated thread pools |
| **React + Vite** | Fast dev server, modern frontend for visualization |
| **Spring Profiles** | Single codebase runs as controller, client, or worker based on profile |

---

## 2. System Architecture

### Controller-Worker Model

The system follows a **centralized controller** pattern (similar to GFS Master or HDFS NameNode):

```
                    ┌─────────────────────┐
                    │    CONTROLLER        │
                    │    (Port 8085)       │
                    │                     │
                    │  • Worker Registry   │
                    │  • Key Directory     │
                    │  • Heartbeat Monitor │
                    │  • Re-replication    │
                    └────┬───┬───┬───┬────┘
                         │   │   │   │
            Heartbeats   │   │   │   │  Key Mappings
                         │   │   │   │
              ┌──────────┘   │   │   └──────────┐
              │         ┌────┘   └────┐         │
              ▼         ▼             ▼         ▼
         ┌────────┐┌────────┐   ┌────────┐┌────────┐
         │Worker 1││Worker 2│   │Worker 3││Worker 4│
         │ :8081  ││ :8082  │   │ :8083  ││ :8084  │
         └────────┘└────────┘   └────────┘└────────┘

              ┌─────────────────────────┐
              │    CLIENT SERVICE       │
              │    (Port 8080)          │
              │  Orchestrates PUT/GET   │
              └─────────────────────────┘
```

### Why Centralized Controller?

**Advantages:**
- Simpler design — single source of truth for metadata
- Easier to reason about consistency of key mappings
- Straightforward failure detection from one point

**Trade-off:**
- Controller is a single point of failure (assumed always-up in this project)
- In production systems, you'd use Raft/Paxos for controller HA

### Multi-Profile Single Codebase

A clever design decision — the entire system runs from **one Spring Boot application** with different Spring profiles:

```properties
# application-controller.properties → port 8085
# application-worker1.properties   → port 8081
# application-client.properties    → port 8080
```

The `HeartBeatService` only activates on worker profiles via `@Profile("worker")`, preventing the controller/client from sending heartbeats.

---

## 3. Core Distributed Systems Concepts

### 3.1 Data Partitioning (Sharding)

**How keys are assigned to workers:**

```java
int hash = Math.abs(key.hashCode());
WorkerInfo primary = aliveWorkers.get(hash % aliveWorkers.size());
```

This is **hash-based partitioning** — the key's hash determines which worker is primary. This distributes keys roughly uniformly across workers.

**Comparison with real systems:**
- **Consistent Hashing** (Dynamo, Cassandra) — handles node addition/removal better by minimizing key reassignment
- **Range Partitioning** (HBase, Spanner) — keys are divided into ranges, good for range queries
- **Hash Partitioning** (this project) — simple, uniform distribution, but ALL keys remap when worker count changes

### 3.2 Replication

Each key is stored on **3 workers** (replication factor = 3):
1. **Primary** — selected via hash partitioning
2. **Replica 1** — synchronous write (strong durability)
3. **Replica 2** — asynchronous write with 5-second delay (eventual consistency)

### 3.3 Consistency Model

This system implements a **hybrid consistency model**:
- **Write path**: Quorum-like — success requires primary + 1 sync replica (2 out of 3)
- **Read path**: Reads from primary only — strong consistency for reads *if primary is alive*
- **Overall**: Between strong and eventual consistency

### 3.4 Fault Tolerance

- **Heartbeat-based failure detection** with 10-second timeout
- **Automatic re-replication** restores replication factor after failure
- **Worker rejoin** — failed workers can re-register and become active

---

## 4. Detailed Component Design

### 4.1 ControllerService — The Brain

```
ControllerService
├── workers: ConcurrentHashMap<String, WorkerInfo>     // Worker registry
├── workerList: synchronized ArrayList<WorkerInfo>      // Ordered worker list
├── keyDirectory: ConcurrentHashMap<String, RouteResponse>  // Global key→mapping
│
├── registerWorker()        // Add/update worker in registry
├── getKeyMapping()         // Hash-based primary selection + replica assignment
├── updateHeartbeat()       // Mark worker alive, update timestamp
├── startHeartbeatChecker() // @PostConstruct — runs every 5s
└── triggerReReplication()  // Redistribute data from dead worker
```

**Key insight**: The `keyDirectory` is the global metadata store. Every PUT operation updates it, and failure recovery reads it to know which keys were on a dead worker.

### 4.2 WorkerService — The Storage Engine

```
WorkerService
├── store: ConcurrentHashMap<String, String>  // Local KV store
│
├── put(key, value)        // Local write + sync replica1 + async replica2
├── get(key)               // Simple local lookup
└── storeReplica(key, record) // Accept replicated data
```

**Key insight**: Workers are both storage nodes AND replication initiators. When a worker receives a PUT, it doesn't just store locally — it actively replicates to other workers.

### 4.3 ClientService — The Orchestrator

```
ClientService
├── put(key, value)   // Controller lookup → forward to primary worker
├── get(key)          // Controller lookup → return primary worker info
└── getVal(key, ...)  // Direct worker contact → return value
```

**Key insight**: The GET is a **two-phase operation**:
1. Ask controller "who has this key?" → get `WorkerInfo`
2. Contact that worker directly → get the value

This separates metadata routing from data access, similar to how HDFS separates NameNode from DataNodes.

### 4.4 HeartBeatService — The Pulse

```java
@Service
@Profile("worker")  // Only runs on worker instances
public class HeartBeatService {
    @Scheduled(fixedRate = 5000)  // Every 5 seconds
    public void sendHeartbeat() {
        // POST {id: workerId} to controller
    }
}
```

**Key insight**: `@Profile("worker")` is critical — without it, the controller and client would also try to send heartbeats, causing errors.

---

## 5. Data Flow — PUT Operation

### Step-by-Step Walkthrough

```
Step 1: Client sends POST /v1/client/put {"key":"user:42", "value":"John"}
                              │
Step 2: ClientService asks Controller for key mapping
        GET /v1/controller/key-mapping/user:42
        Controller computes: hash("user:42") % 4 = 2 → Worker-3 is primary
        Returns: { primary: worker-3, replicas: [worker-1, worker-2] }
                              │
Step 3: ClientService forwards PUT to primary worker
        POST http://localhost:8083/v1/worker/put {"key":"user:42","value":"John"}
                              │
Step 4: Worker-3 (primary) stores locally
        store.put("user:42", "John")  // ConcurrentHashMap
                              │
Step 5: Worker-3 replicates SYNCHRONOUSLY to Replica 1 (Worker-1)
        PUT http://localhost:8081/v1/worker/replicate/user:42
        Body: {"key":"user:42", "value":"John"}
        ⏳ BLOCKS until Worker-1 confirms
                              │
Step 6: Worker-3 schedules ASYNC replication to Replica 2 (Worker-2)
        CompletableFuture.runAsync(() -> {
            PUT http://localhost:8082/v1/worker/replicate/user:42
            POST /v1/controller/replica/ack  // Notify controller
        }, delayedExecutor(5, SECONDS));
        ⚡ DOES NOT BLOCK — returns immediately
                              │
Step 7: Response sent back to client
        success = (localWrite + syncReplica >= 2)
        {"success": true, "message": "Replica1 written to worker-1"}
```

### Why This Design?

- **Sync Replica 1**: Ensures at least 2 copies exist before confirming to client → durability guarantee
- **Async Replica 2**: Third copy for extra safety, but doesn't slow down the write path
- **5-second delay**: Spreads out network load, allows batching in theory

---

## 6. Data Flow — GET Operation

```
Step 1: Client sends GET /v1/client/get?key=user:42
                              │
Step 2: ClientService asks Controller for primary worker
        GET /v1/controller/key-mapping/user:42
        Returns: WorkerInfo { id:"worker-3", host:"localhost", port:8083 }
                              │
Step 3: Client receives WorkerInfo (primary worker details)
        Frontend now knows: worker-3 at localhost:8083 has this key
                              │
Step 4: Client sends GET /v1/client/get/val?key=user:42&id=worker-3&host=localhost&port=8083
                              │
Step 5: ClientService contacts worker-3 directly
        GET http://localhost:8083/v1/worker/get?key=user:42
        Worker-3 does: store.get("user:42") → "John"
                              │
Step 6: Returns {"key":"user:42", "value":"John"}
```

### Why Two-Phase GET?

The GET is split into two API calls so the frontend can **display the routing information** (which worker is primary) before fetching the actual value. This is a pedagogical design choice to make the distributed nature visible in the UI.

---

## 7. Replication Strategy

### Synchronous vs Asynchronous Replication

| Aspect | Sync (Replica 1) | Async (Replica 2) |
|--------|-------------------|---------------------|
| **Blocking** | Yes — waits for ACK | No — fire-and-forget with delay |
| **Latency impact** | Adds network RTT to write | Zero impact on write latency |
| **Durability** | Guaranteed before response | Eventually consistent |
| **Failure window** | None | Up to 5 seconds of data loss |
| **Implementation** | `restTemplate.put()` | `CompletableFuture.runAsync()` with `delayedExecutor(5s)` |

### Write Quorum Analysis

```
Replication Factor (N) = 3
Sync Writes (W) = 2 (primary + replica1)
Read Path (R) = 1 (primary only)

W + R = 3 = N → Quorum satisfied for strong consistency
                 (but only if reads always go to primary)
```

### Code Deep-Dive: Replication in WorkerService

```java
// Synchronous replication — BLOCKS
restTemplate.put(replica1Url, record);
successCount++;

// Asynchronous replication — DOES NOT BLOCK
CompletableFuture.runAsync(() -> {
    restTemplate.put(replica2Url, record);
    // Notify controller that async replication completed
    restTemplate.postForObject(ackUrl, null, String.class);
}, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));

// Success = local write + at least 1 replica
boolean success = successCount >= 2;
```

---

## 8. Failure Detection & Heartbeats

### How It Works

```
Worker Lifecycle:
  REGISTER → ALIVE → (missed heartbeat) → DEAD → (re-register) → ALIVE

Timeline:
  t=0s    Worker sends heartbeat
  t=5s    Worker sends heartbeat      Controller checks (alive)
  t=10s   Worker sends heartbeat      Controller checks (alive)
  t=15s   ❌ Worker crashes
  t=15s   Controller checks           (now - lastHB = 5s → alive)
  t=20s   Controller checks           (now - lastHB = 10s → DEAD!)
  t=20s   triggerReReplication() called
```

### Implementation Details

```java
@PostConstruct
public void startHeartbeatChecker() {
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (WorkerInfo w : workers.values()) {
                if (now - w.getLastHeartbeat() > 10000 && w.isAlive()) {
                    w.setAlive(false);
                    triggerReReplication(w.getId());
                } else if (now - w.getLastHeartbeat() <= 10000) {
                    w.setAlive(true);  // Auto-recovery
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
}
```

### Key Parameters

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Heartbeat interval | 5 seconds | Frequent enough for quick detection |
| Failure timeout | 10 seconds | 2 missed heartbeats = failure |
| Detection delay | 5–15 seconds worst case | Balance between false positives and detection speed |

### Failure Detection Theory

This is a **push-based heartbeat** model (workers push to controller), as opposed to:
- **Pull-based** (controller polls workers) — more network traffic from controller
- **Gossip-based** (peers exchange info) — used in Cassandra, more scalable but complex
- **Phi Accrual** (adaptive threshold) — used in Akka, adjusts timeout dynamically

---

## 9. Re-Replication & Self-Healing

### What Happens When a Worker Dies

```
1. Controller detects worker-2 is dead

2. Controller scans keyDirectory:
   Key "user:42" → primary: worker-3, replicas: [worker-1, worker-2]
                                                          ^^^^^^^^
                                                          AFFECTED!

3. Find a surviving source that has the data:
   worker-3 (primary) → ALIVE ✅ → use as source

4. Find a new target that doesn't have the data:
   worker-4 → not in [worker-3, worker-1, worker-2] → selected

5. Transfer: GET value from worker-3, PUT to worker-4

6. Update mapping:
   Before: primary: worker-3, replicas: [worker-1, worker-2]
   After:  primary: worker-3, replicas: [worker-1, worker-4]
```

### Code: triggerReReplication()

```java
public synchronized void triggerReReplication(String deadWorkerId) {
    for (Map.Entry<String, RouteResponse> entry : keyDirectory.entrySet()) {
        String key = entry.getKey();
        RouteResponse route = entry.getValue();

        // Check if this key was on the dead worker
        boolean affected = route.getPrimary().getId().equals(deadWorkerId)
            || route.getReplicas().stream()
                .anyMatch(w -> w.getId().equals(deadWorkerId));

        if (!affected) continue;

        // Find alive source → Find alive target → Transfer data → Update mapping
    }
}
```

### Edge Cases Handled

- **Dead worker was primary**: New primary is selected from alive workers
- **Dead worker was replica**: New replica replaces it in the mapping
- **No source available**: Logged as warning, key data may be lost
- **No target available**: Logged as warning, replication factor temporarily reduced

---

## 10. Concurrency & Thread Safety

### Thread Safety Mechanisms Used

| Data Structure | Type | Why |
|---|---|---|
| `workers` map | `ConcurrentHashMap` | Multiple threads read/write worker registry |
| `workerList` | `Collections.synchronizedList` | Iterated during key mapping |
| `keyDirectory` | `ConcurrentHashMap` | Updated during PUT and re-replication |
| Worker `store` | `ConcurrentHashMap` | Concurrent reads/writes from different clients |

### Concurrency Scenarios

1. **Simultaneous PUTs to same key**: `ConcurrentHashMap.put()` is atomic — last writer wins
2. **Heartbeat + Re-replication**: `triggerReReplication` is `synchronized` to prevent concurrent re-replication
3. **Async replication + Read**: Possible stale read from replica 2 during the 5-second delay window

### Potential Race Condition

```
Thread A: PUT("key1", "v1") → starts async replication to replica2
Thread B: PUT("key1", "v2") → starts async replication to replica2

If Thread A's async completes AFTER Thread B's:
  Primary has "v2", Replica1 has "v2", but Replica2 has "v1" ← STALE!
```

This is a known trade-off of async replication — acceptable for this project's scope.

---

## 11. API Design & REST Conventions

### Three Controller Layers

The project cleanly separates three REST controllers:

| Controller | Prefix | Responsibility |
|---|---|---|
| `ClientController` | `/v1/client` | External API for end users |
| `Controller` | `/v1/controller` | Internal API for worker management |
| `WorkerController` | `/v1/worker` | Internal API for data operations |

### API Versioning

The `/v1/` prefix enables future API evolution without breaking existing clients.

### CORS Configuration

```java
registry.addMapping("/**")
    .allowedOrigins("http://localhost:5173")  // Vite dev server
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowCredentials(true);
```

---

## 12. Frontend Dashboard

### Architecture

The React frontend provides **separate dashboards** for each system component:

| Route | Component | Connects To |
|---|---|---|
| `/client` | `Client.jsx` | Client service (port 8080) |
| `/controller` | `ControllerFrontend.jsx` | Controller (port 8085) |
| `/worker` | `WorkerFrontend.jsx` | Worker 1 (port 8081) |
| `/worker2` | `WorkerFrontend2.jsx` | Worker 2 (port 8082) |
| `/worker3` | `WorkerFrontend3.jsx` | Worker 3 (port 8083) |
| `/worker4` | `WorkerFrontend4.jsx` | Worker 4 (port 8084) |

### Key Frontend Features

- **Client page**: PUT key-value pairs, discover primary workers, fetch values
- **Controller page**: Register workers manually, lookup key mappings, view all workers and their alive/dead status
- **Worker pages**: Check status, read/write directly to individual worker stores (useful for verifying replication)

---

## 13. Trade-offs, Limitations & Interview Talking Points

### Design Trade-offs Made

| Decision | Trade-off |
|---|---|
| **Centralized controller** | Simpler but single point of failure |
| **In-memory storage** | Fast but data lost on restart |
| **Hash partitioning** | Simple but poor rebalancing on node changes |
| **Sync + Async replication** | Good latency but 5s window for data loss |
| **RestTemplate (sync HTTP)** | Simple but blocking — limits throughput |
| **Fixed 4 workers** | Predictable but not dynamically scalable |

### Known Limitations

1. **No persistence** — All data is in `ConcurrentHashMap`, lost on restart
2. **No consistency protocol** — No vector clocks, no conflict resolution
3. **Controller SPOF** — No leader election or controller replication
4. **No client-side caching** — Every read goes to the network
5. **Static worker count** — Adding/removing workers requires code changes
6. **No authentication** — All endpoints are open

### How You'd Improve It (Great Interview Answer)

1. **Add persistence**: Use RocksDB or LevelDB as the storage engine per worker
2. **Consistent hashing**: Replace modulo-based partitioning with a hash ring for dynamic scaling
3. **Controller HA**: Use Raft consensus (e.g., via etcd or custom implementation) for controller replication
4. **Write-Ahead Log (WAL)**: Log writes before applying them for crash recovery
5. **Read replicas**: Allow reads from replicas with configurable consistency levels
6. **gRPC**: Replace REST with gRPC for lower-latency inter-service communication
7. **Gossip protocol**: Replace centralized heartbeats with decentralized failure detection

---

## 14. Potential Interview Questions & Answers

### Q1: Why did you choose a replication factor of 3?

**Answer**: Industry standard (used by HDFS, Cassandra, etc.). With RF=3, the system tolerates 1 node failure with no data loss and 2 node failures with potential data loss. It's a balance between storage overhead (3x) and durability. Our write quorum of 2 (primary + sync replica) ensures data survives any single node failure.

### Q2: What happens if the controller goes down?

**Answer**: In our design, the controller is assumed to be always available (single point of failure). Workers continue to serve reads for existing data, but new PUTs will fail because the client can't discover primary workers. No new heartbeats are processed, so worker failures won't be detected. To fix this, I'd implement controller replication using Raft consensus.

### Q3: How do you handle network partitions?

**Answer**: Currently, we don't explicitly handle network partitions. If a worker can't reach the controller, it's marked dead even if it's still running — this is a **false positive**. Per CAP theorem, our system prioritizes **CP** (consistency + partition tolerance) over availability, because we'd rather mark a node dead than serve stale data.

### Q4: Why synchronous + asynchronous replication instead of all synchronous?

**Answer**: Pure synchronous replication (waiting for all 3 writes) would increase write latency by 2x network RTT. Our hybrid approach ensures durability (2 copies confirmed) while keeping latency close to a single-replica system. The async third copy provides defense-in-depth — if both the primary and sync replica fail within 5 seconds, we'd lose data, but that's extremely unlikely.

### Q5: How does your key-mapping work? What if a worker joins or leaves?

**Answer**: We use `hash(key) % numAliveWorkers` for primary selection. The limitation is that when the worker count changes, many keys remap to different workers (similar to naive hash partitioning in databases). Consistent hashing would solve this — only K/N keys need to move when a node is added/removed.

### Q6: What consistency guarantees does your system provide?

**Answer**: We provide **read-your-writes consistency** for the primary worker path: after a successful PUT, a GET to the same primary will return the new value. However, reading from replicas may return stale data (up to 5 seconds for replica 2). This is similar to Amazon DynamoDB's "eventually consistent reads" vs "strongly consistent reads."

### Q7: How would you scale this to 1000 nodes?

**Answer**: Several changes needed: (1) Replace centralized controller with a distributed metadata service using Raft, (2) Use consistent hashing with virtual nodes for better load balancing, (3) Replace REST with gRPC for lower overhead, (4) Implement gossip-based failure detection instead of centralized heartbeats, (5) Add connection pooling and async HTTP clients.

### Q8: Why did you use Spring Profiles instead of separate microservices?

**Answer**: For development convenience — one codebase, one build artifact. In production, you'd likely separate them, but for a course project, profiles let us share DTOs, avoid code duplication, and simplify the build. The `@Profile("worker")` annotation on `HeartBeatService` ensures heartbeats only run on worker instances.

### Q9: Explain the `CompletableFuture.delayedExecutor(5, SECONDS)` usage.

**Answer**: `CompletableFuture.runAsync(task, executor)` runs the task on the provided executor. `delayedExecutor(5, SECONDS)` wraps the default ForkJoinPool to delay execution by 5 seconds. This means the async replication task is submitted immediately but only executes after a 5-second delay, simulating a background batch replication without blocking the PUT response.

### Q10: How do you prevent the same key from being replicated to the same worker?

**Answer**: In `getKeyMapping()`, replicas are selected from alive workers that are NOT the primary:

```java
for (WorkerInfo w : aliveWorkers) {
    if (!w.getId().equals(primary.getId()))
        replicas.add(w);
    if (replicas.size() >= REPLICATION_FACTOR - 1) break;
}
```

This ensures the primary and replica sets are disjoint. During re-replication, a similar check finds a target worker not in the current holders list.

---

## 15. CAP Theorem Analysis

### Where Does This System Sit?

```
        Consistency
           /\
          /  \
         /    \
        / THIS \
       / SYSTEM \
      /    (CP)  \
     /____________\
Availability    Partition
                Tolerance
```

**Consistency**: YES — Reads go to primary, writes require quorum (2/3)
**Partition Tolerance**: YES — Controller marks unreachable workers as dead, re-replicates
**Availability**: PARTIAL — Unavailable during controller downtime or when all replicas for a key are down

### Comparison with Real Systems

| System | CAP | Replication | Consistency |
|--------|-----|-------------|-------------|
| **This Project** | CP | 3 replicas, sync+async | Primary-read strong |
| **Cassandra** | AP | Tunable RF, async | Tunable (QUORUM, ONE, ALL) |
| **DynamoDB** | AP | 3 replicas, async | Eventually consistent + strongly consistent reads |
| **etcd** | CP | Raft consensus | Linearizable |
| **Redis Cluster** | CP | Async master-replica | Strong on master reads |

---

> **Final tip for the interview**: Always frame your answers around trade-offs. Every design decision has pros and cons — the interviewer wants to see that you understand *why* you made a choice, not just *what* you built.
