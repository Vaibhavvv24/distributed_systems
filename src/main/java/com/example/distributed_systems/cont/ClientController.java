package com.example.distributedkv.client;

import com.example.distributedkv.common.dto.KVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/client")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientRestController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Client GET: returns value for key (reads from primary; falls back to replicas if primary unreachable).
     */
    @GetMapping("/kv/{key}")
    public ResponseEntity<KVRecord> getKey(@PathVariable String key) {
        KVRecord record = clientService.get(key);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * Client PUT: store a value for key. Body: { "value": "..." }
     * Returns 200 on success, 500 if put could not achieve write quorum / primary unreachable.
     */
    @PutMapping("/kv/{key}")
    public ResponseEntity<Void> putKey(@PathVariable String key, @RequestBody PutRequest body) {
        boolean ok = clientService.put(key, body.getValue());
        if (ok) return ResponseEntity.ok().build();
        return ResponseEntity.status(500).build();
    }

    // simple DTO for incoming put body
    public static class PutRequest {
        private String value;
        public PutRequest() {}
        public PutRequest(String value) { this.value = value; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
