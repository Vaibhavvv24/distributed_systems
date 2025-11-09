package com.example.distributed_systems.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.distributed_systems.dto.HeartBeatRequest;
import com.example.distributed_systems.dto.HeartBeatResponse;

@Service
public class HeartBeatService {

    @Autowired
    private RestTemplate restTemplate;

    private final String CONTROLLER_URL = "http://localhost:8080/controller/heartbeat";
    private final String WORKER_ID = System.getProperty("worker.id", "worker-1");

    // Send heartbeat every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        HeartBeatRequest req = new HeartBeatRequest();
        req.setId(WORKER_ID);
        try{
            restTemplate.postForObject(CONTROLLER_URL, req, HeartBeatResponse.class);
            System.out.println("Heartbeat sent by " + WORKER_ID);
        } catch (Exception e) {
            System.out.println("Failed to send heartbeat: " + e.getMessage());
        }
    }
}

