package com.example.distributed_systems.service;


import com.example.distributed_systems.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class ClientService {

    @Autowired
    private RestTemplate restTemplate;

    private final String CONTROLLER_URL = "http://localhost:8085/v1/controller/key-mapping";

 
public ClientPutResponse put(String key, String value) {


    RouteResponse mapping = restTemplate.getForObject(
        CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8),
        RouteResponse.class
    );

    if (mapping == null || mapping.getPrimary() == null) {
        return new ClientPutResponse(false, "No primary available");
    }

    WorkerInfo primary = mapping.getPrimary();

    try {
        String primaryUrl = "http://" + primary.getHost() + ":" + primary.getPort() + "/v1/worker/put";
        PutRequest req = new PutRequest();
        req.setKey(key);
        req.setValue(value);

       
        PutResponse resp = restTemplate.postForObject(primaryUrl, req, PutResponse.class);
        if (resp != null && resp.isSuccess()) {
            return new ClientPutResponse(true, "Data written successfully");
        } else {
 
            String msg = (resp == null) ? "Primary returned no response" : "Primary reported failure: " + resp.isSuccess();
            return new ClientPutResponse(false, "Write failed: " + msg);
        }
    } catch (Exception e) {

        return new ClientPutResponse(false, "Primary unavailable: " + e.getMessage());
    }
}


//    public ClientGetResponse get(String key) {
//     RouteResponse mapping = restTemplate.getForObject(
//         CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8),
//         RouteResponse.class
//     );

//     if (mapping == null || mapping.getPrimary() == null) {
//         return new ClientGetResponse(key, "No primary available");
//     }

//     WorkerInfo primary = mapping.getPrimary();
//     String url = "http://" + primary.getHost() + ":" + primary.getPort() +
//                  "/v1/worker/get?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

//     try {
//         GetResponse resp = restTemplate.getForObject(url, GetResponse.class);
//         if (resp != null) {
//             System.out.println("🟢 Read value from primary: " + primary.getId());
//             return new ClientGetResponse(key, resp.getValue());
//         } else {
//             return new ClientGetResponse(key, "Not found");
//         }
//     } catch (Exception e) {
//         System.out.println("❌ Primary read failed on " + primary.getId() + ": " + e.getMessage());
//         return new ClientGetResponse(key, "Primary unavailable");
//     }
// }

public WorkerInfo get(String key) {
    RouteResponse mapping = restTemplate.getForObject(
        CONTROLLER_URL + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8),
        RouteResponse.class
    );

    if (mapping == null || mapping.getPrimary() == null) {
        return null;
    }
    WorkerInfo primary = mapping.getPrimary();
    System.out.println("🟢 Key mapped to primary: " + primary.getId());
    return primary;
}

public ClientGetResponse getVal(String key,String id,String host,int port) {
    String url = "http://" + host + ":" + port +
                 "/v1/worker/get?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

    try {
        GetResponse resp = restTemplate.getForObject(url, GetResponse.class);
        if (resp != null) {
            System.out.println("🟢 Read value from primary: " + id);
            return new ClientGetResponse(key, resp.getValue());
        } else {
            return new ClientGetResponse(key, "Not found");
        }
    } catch (Exception e) {
        System.out.println("❌ Primary read failed on " + id + ": " + e.getMessage());
        return new ClientGetResponse(key, "Primary unavailable");
    }
    
}
}
