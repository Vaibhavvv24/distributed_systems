# 🗄️ Distributed Key-Value Store

A fault-tolerant, replicated distributed key-value store built with **Spring Boot** (Java 17) and a **React + Vite** frontend dashboard. The system implements a controller-worker architecture with heartbeat-based failure detection, synchronous + asynchronous replication, and automatic re-replication on worker failure.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      CLIENT (React)                     │
│                    localhost:5173                        │
└──────────┬──────────────────────────────┬───────────────┘
           │  PUT/GET requests            │
           ▼                              ▼
┌─────────────────────┐       ┌─────────────────────────┐
│   Client Service    │       │   Controller Service    │
│   localhost:8080    │◄─────►│   localhost:8085         │
│  (Spring Profile:   │       │  (Spring Profile:       │
│   client)           │       │   controller)           │
└─────────────────────┘       └───────┬─────────────────┘
                                      │  Heartbeats / Key Mapping
              ┌───────────┬───────────┼───────────┐
              ▼           ▼           ▼           ▼
        ┌──────────┐┌──────────┐┌──────────┐┌──────────┐
        │ Worker 1 ││ Worker 2 ││ Worker 3 ││ Worker 4 │
        │ :8081    ││ :8082    ││ :8083    ││ :8084    │
        └──────────┘└──────────┘└──────────┘└──────────┘
```

### Key Design Decisions

| Concept | Implementation |
|---|---|
| **Replication Factor** | 3 (1 primary + 2 replicas) |
| **Primary Selection** | Hash-based partitioning (`key.hashCode() % aliveWorkers`) |
| **Sync Replication** | First replica — blocks until write confirmed |
| **Async Replication** | Second replica — written after 5-second delay via `CompletableFuture` |
| **Failure Detection** | Heartbeat timeout of 10 seconds, checked every 5 seconds |
| **Re-replication** | Automatic on worker failure; controller transfers data from surviving replicas |
| **Data Storage** | In-memory `ConcurrentHashMap` per worker |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Spring Boot 3.5.7 |
| **Language** | Java 17 |
| **Build Tool** | Maven |
| **Frontend** | React 19 + Vite (rolldown-vite) |
| **Styling** | Tailwind CSS 3.4 |
| **Routing** | React Router v7 |
| **HTTP Client (Backend)** | Spring `RestTemplate` |
| **Concurrency** | `ConcurrentHashMap`, `CompletableFuture`, `ScheduledExecutorService` |

---

## 📁 Project Structure

```
distributed_systems/
├── Backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/distributed_systems/
│       │   ├── DistributedSystemsApplication.java   # Entry point (@EnableScheduling)
│       │   ├── config/
│       │   │   ├── CorsConfig.java                  # CORS for frontend (localhost:5173)
│       │   │   ├── RestTemplateConfig.java           # RestTemplate bean
│       │   │   └── WorkerConfig.java                 # Static replica URL map
│       │   ├── cont/
│       │   │   ├── ClientController.java             # /v1/client/* endpoints
│       │   │   ├── Controller.java                   # /v1/controller/* endpoints
│       │   │   └── WorkerController.java             # /v1/worker/* endpoints
│       │   ├── dto/
│       │   │   ├── ClientPutRequest.java             # Client → PUT payload
│       │   │   ├── ClientPutResponse.java            # PUT result (success + message)
│       │   │   ├── ClientGetResponse.java            # GET result (key + value)
│       │   │   ├── GetRequest.java / GetResponse.java
│       │   │   ├── PutRequest.java / PutResponse.java
│       │   │   ├── HeartBeatRequest.java / HeartBeatResponse.java
│       │   │   ├── KVRecord.java                     # Key-value pair for replication
│       │   │   ├── RouteResponse.java                # Primary + replicas mapping
│       │   │   └── WorkerInfo.java                   # Worker metadata (id, host, port, alive, lastHeartbeat)
│       │   └── service/
│       │       ├── ClientService.java                # Client-side PUT/GET orchestration
│       │       ├── ControllerService.java            # Worker registry, key mapping, re-replication
│       │       ├── HeartBeatService.java             # Periodic heartbeat sender (worker profile only)
│       │       └── WorkerService.java                # Local store, replication logic
│       └── resources/
│           ├── application-controller.properties     # port=8085
│           ├── application-client.properties         # port=8080
│           ├── application-worker1.properties        # port=8081
│           ├── application-worker2.properties        # port=8082
│           ├── application-worker3.properties        # port=8083
│           └── application-worker4.properties        # port=8084
│
└── Frontend/
    └── dis-frontend/
        ├── package.json
        ├── vite.config.js
        └── src/
            ├── App.jsx                               # React Router setup
            ├── main.jsx                              # Entry point
            └── pages/
                ├── Client.jsx                        # Client dashboard (PUT/GET)
                ├── ControllerFrontend.jsx             # Controller dashboard
                ├── WorkerFrontend.jsx                 # Worker 1 dashboard (:8081)
                ├── WorkerFrontend2.jsx                # Worker 2 dashboard (:8082)
                ├── WorkerFrontend3.jsx                # Worker 3 dashboard (:8083)
                └── WorkerFrontend4.jsx                # Worker 4 dashboard (:8084)
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** and **Maven 3.8+**
- **Node.js 18+** and **npm 9+**

### 1. Start the Controller (port 8085)

```bash
cd Backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=controller
```

### 2. Start Workers (ports 8081–8084)

Open four separate terminals:

```bash
# Terminal 1
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker,worker1

# Terminal 2
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker,worker2

# Terminal 3
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker,worker3

# Terminal 4
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker,worker4
```

### 3. Start the Client Service (port 8080)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=client
```

### 4. Start the Frontend (port 5173)

```bash
cd Frontend/dis-frontend
npm install
npm run dev
```

### 5. Register Workers with the Controller

```bash
curl -X POST "http://localhost:8085/v1/controller/register?id=worker-1&host=localhost&port=8081"
curl -X POST "http://localhost:8085/v1/controller/register?id=worker-2&host=localhost&port=8082"
curl -X POST "http://localhost:8085/v1/controller/register?id=worker-3&host=localhost&port=8083"
curl -X POST "http://localhost:8085/v1/controller/register?id=worker-4&host=localhost&port=8084"
```

---

## 🌐 API Reference

### Client Endpoints (`/v1/client`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/client/put` | Store a key-value pair (body: `{"key":"...", "value":"..."}`) |
| `GET` | `/v1/client/get?key=...` | Get primary worker info for a key |
| `GET` | `/v1/client/get/val?key=...&id=...&host=...&port=...` | Retrieve value from a specific worker |

### Controller Endpoints (`/v1/controller`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/controller/register?id=...&host=...&port=...` | Register a worker |
| `POST` | `/v1/controller/heartbeat` | Receive heartbeat (body: `{"id":"..."}`) |
| `GET` | `/v1/controller/key-mapping/{key}` | Get primary + replica workers for a key |
| `GET` | `/v1/controller/workers` | List all registered workers |
| `POST` | `/v1/controller/replica/ack?key=...&replicaId=...` | Acknowledge async replication |

### Worker Endpoints (`/v1/worker`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/v1/worker/status` | Check if worker is running |
| `GET` | `/v1/worker/get?key=...` | Get a value from local store |
| `POST` | `/v1/worker/put` | Store key-value + trigger replication |
| `PUT` | `/v1/worker/replicate/{key}` | Receive replicated data |
| `GET` | `/v1/worker/health` | Health check endpoint |

---

## 🖥️ Frontend Pages

| Route | Page | Description |
|---|---|---|
| `/` | Home | Landing page |
| `/client` | Client Dashboard | PUT key-value pairs, GET primary worker info, fetch values |
| `/controller` | Controller Dashboard | Register workers, lookup key mappings, list all workers |
| `/worker` | Worker 1 Dashboard | Status, GET/PUT on worker-1 (port 8081) |
| `/worker2` | Worker 2 Dashboard | Status, GET/PUT on worker-2 (port 8082) |
| `/worker3` | Worker 3 Dashboard | Status, GET/PUT on worker-3 (port 8083) |
| `/worker4` | Worker 4 Dashboard | Status, GET/PUT on worker-4 (port 8084) |

---

## 🔄 System Flows

### PUT Operation
```
Client → ClientService → Controller (get key mapping)
                        → Primary Worker (local write)
                        → Replica 1 (synchronous write, blocks)
                        → Replica 2 (async write, 5s delay via CompletableFuture)
                        → Controller ACK (async replication complete)
```

### GET Operation
```
Client → ClientService → Controller (discover primary worker)
       ← WorkerInfo returned to client
Client → Worker (fetch value from local store)
       ← Key-Value returned
```

### Failure Detection & Recovery
```
Workers → Controller (heartbeat every 5s)
Controller → checks heartbeats every 5s
  If (now - lastHeartbeat > 10s):
    → Mark worker DEAD
    → For each key on dead worker:
      → Find surviving replica with data
      → Select new alive worker
      → Transfer key-value to new worker
      → Update key directory mappings
```

---

## 🧪 Testing

Run the built-in test suite:

```bash
cd Backend
./mvnw test
```

### Manual Testing Scenarios

1. **Happy path**: PUT a key, GET it back, verify on worker dashboards
2. **Failure simulation**: Kill a worker process, observe heartbeat timeout, verify re-replication in controller logs
3. **Worker rejoin**: Restart a killed worker, re-register it, verify it becomes active again

---

## 👥 Contributors

- **Aaditya Joshi** — Controller logic
- **Vaibhav Mittal** — Client and Worker logic

---

## 📄 License

This project was developed as a Cloud Computing / Distributed Systems course project.
