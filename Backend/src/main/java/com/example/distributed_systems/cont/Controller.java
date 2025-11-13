package com.example.distributed_systems.cont;
import com.example.distributed_systems.config.RestTemplateConfig;
import com.example.distributed_systems.dto.HeartBeatRequest;
import com.example.distributed_systems.dto.HeartBeatResponse;
import com.example.distributed_systems.dto.RouteResponse;
import com.example.distributed_systems.dto.WorkerInfo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.distributed_systems.service.ClientService;
import com.example.distributed_systems.service.ControllerService;

@RestController
@RequestMapping("/v1/controller")
public class Controller {


    private final ControllerService controllerService;
    @Autowired
    public Controller(ControllerService controllerService) {
        this.controllerService = controllerService;
    }
     @PostMapping("/register")
    public String registerWorker(@RequestParam String id,
                                 @RequestParam String host,
                                 @RequestParam int port) {
        controllerService.registerWorker(id, host, port);
        return "Worker registered: " + id;
    }


    @PostMapping("/heartbeat")
    public ResponseEntity<HeartBeatResponse> heartbeat(@RequestBody HeartBeatRequest req) {
      controllerService.updateHeartbeat(req.getId());
        return ResponseEntity.ok(new HeartBeatResponse(true, "Heartbeat received for " + req.getId()));
    }

    @GetMapping("/key-mapping/{key}")
    public ResponseEntity<RouteResponse> getKeyMapping(@PathVariable String key) {
        RouteResponse resp = controllerService.getKeyMapping(key);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/workers")
    public ResponseEntity<List<WorkerInfo>> listWorkers() {
        List<WorkerInfo> list = controllerService.listWorkers();
        return ResponseEntity.ok(list);
    }
    @PostMapping("/replica/ack")
public ResponseEntity<String> replicaAck(@RequestParam String key, @RequestParam String replicaId) {
    System.out.println("✅ Async replication confirmed for key '" + key + "' on replica " + replicaId);
    return ResponseEntity.ok("Acknowledged");
}

    
}
