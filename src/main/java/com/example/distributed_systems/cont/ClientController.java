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
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping("/put")
    public ClientPutResponse put(@RequestBody ClientPutRequest request) {
        return clientService.put(request.getKey(), request.getValue());
    }

    @GetMapping("/get")
    public ClientGetResponse get(@RequestParam String key) {
        return clientService.get(key);
    }
}
