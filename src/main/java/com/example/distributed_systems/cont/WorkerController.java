package com.example.distributedkv.worker;

import com.example.distributedkv.common.dto.KVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/worker")
public class WorkerController {

    @Autowired
    private WorkerService workerService;

    // Client GET: get value for key
    @GetMapping("/kv/{key}")
    public ResponseEntity<KVRecord> getValue(@PathVariable String key) {
        KVRecord record = workerService.get(key);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    // Client PUT: put value for key
    @PutMapping("/kv/{key}")
    public ResponseEntity<Void> putValue(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }
        boolean success = workerService.handleClientPut(key, value);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(500).build();
        }
    }

    // Called by primary worker to replicate synchronously
    @PutMapping("/replicate/{key}")
    public ResponseEntity<Void> replicateKey(@PathVariable String key, @RequestBody KVRecord record) {
        workerService.storeReplica(key, record);
        return ResponseEntity.ok().build();
    }

    // Bulk replication endpoint (called by controller/donor)
    @PostMapping("/replicate/bulk")
    public ResponseEntity<Void> replicateBulk(@RequestBody List<KVRecord> records) {
        workerService.bulkReplicate(records);
        return ResponseEntity.ok().build();
    }

    // Optional: list all keys (for re-replication)
    @GetMapping("/keys")
    public ResponseEntity<List<String>> listKeys() {
        List<String> keys = workerService.listAllKeys();
        return ResponseEntity.ok(keys);
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
