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
