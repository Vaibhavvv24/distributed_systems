package com.example.distributedkv.controller;

import com.example.distributedkv.common.dto.HeartbeatRequest;
import com.example.distributedkv.common.dto.RouteResponse;
import com.example.distributedkv.common.dto.WorkerRegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/controller")
public class Controller {

    @Autowired
    private ControllerService controllerService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerWorker(@RequestBody WorkerRegisterRequest req) {
        controllerService.registerWorker(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody HeartbeatRequest req) {
        controllerService.receiveHeartbeat(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/route/{key}")
    public ResponseEntity<RouteResponse> routeForKey(@PathVariable String key) {
        RouteResponse resp = controllerService.routeForKey(key);
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
