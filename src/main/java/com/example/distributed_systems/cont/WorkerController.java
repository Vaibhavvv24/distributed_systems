package com.example.distributed_systems.cont;
import com.example.distributed_systems.dto.GetResponse;
import com.example.distributed_systems.dto.PutRequest;
import com.example.distributed_systems.dto.GetResponse;
import com.example.distributed_systems.dto.PutResponse;
import com.example.distributed_systems.dto.KVRecord;
import com.example.distributed_systems.service.WorkerService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/v1/worker")
public class WorkerController {

  

    @Autowired
    private WorkerService workerService;

    // Client GET: get value for key
    @GetMapping("/get")
    public GetResponse get(@RequestParam String key) {
        String value = workerService.get(key);
        if (value == null)
            return new GetResponse(key, "Key not found");
        return new GetResponse(key, value);
    }
    @PostMapping("/put")
    public PutResponse put(@RequestBody PutRequest request) {
        return workerService.put(request.getKey(), request.getValue());
    }


    // Called by primary worker to replicate synchronously
    @PutMapping("/replicate/{key}")
    public ResponseEntity<Void> replicateKey(@PathVariable String key, @RequestBody KVRecord record) {
        workerService.storeReplica(key, record);
        return ResponseEntity.ok().build();
    }


    // Health check
    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
