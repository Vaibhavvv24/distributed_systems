package com.example.distributed_systems.cont;
import com.example.distributed_systems.dto.HeartbeatRequest;


import java.util.List;

@RestController
@RequestMapping("/v1/controller")
public class Controller {

    @Autowired
    private ControllerService controllerService;

     @PostMapping("/register")
    public String registerWorker(@RequestParam String id,
                                 @RequestParam String host,
                                 @RequestParam int port) {
        controllerService.registerWorker(id, host, port);
        return "Worker registered: " + id;
    }


    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody HeartbeatRequest req) {
      controllerService.updateHeartbeat(req.getId());
        return new HeartbeatResponse(true, "Heartbeat received for " + req.getId());
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

    @PostMapping("/trigger-rereplicate")
    public ResponseEntity<Void> triggerReReplication() {
        controllerService.triggerReReplication();
        return ResponseEntity.ok().build();
    }
}
