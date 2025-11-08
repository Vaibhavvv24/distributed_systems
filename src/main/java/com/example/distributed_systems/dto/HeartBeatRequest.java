
package com.example.distributed_systems.dto;

public class HeartBeatRequest {
    private String id;
   

    public HeartBeatRequest(){
        
    }
    public HeartBeatRequest(String id) {
        this.id = id;
       
    }
    public String getId() {
        return id;
    }
 
    public void setId(String id) {
        this.id = id;
    }
  
}
