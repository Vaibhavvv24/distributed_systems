package com.example.distributed_systems.cont;
import com.example.distributed_systems.dto.ClientGetResponse;
import com.example.distributed_systems.dto.ClientPutRequest;
import com.example.distributed_systems.dto.ClientPutResponse;
import com.example.distributed_systems.service.ClientService;

// import com.example.distributed_systems.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.example.distributed_systems.dto.WorkerInfo;

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
    public WorkerInfo get(@RequestParam String key) {
        return clientService.get(key);
    }
    @GetMapping("/get/val")
    public ClientGetResponse getVal(@RequestParam String key, @RequestParam String id, @RequestParam String host, @RequestParam int port) {
        return clientService.getVal(key, id, host, port);
    }
}
