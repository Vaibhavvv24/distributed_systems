package com.example.distributed_systems.dto;

public class HeartBeatResponse {
   
    private boolean acknowledged;
    private String message;
   

    public HeartBeatResponse(){
        
    }
    public HeartBeatResponse(boolean acknowledged, String message) {
        this.acknowledged = acknowledged;
        this.message = message;
    }
   
    public boolean isAcknowledged() {
        return acknowledged;
    }

    
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }
  
}
