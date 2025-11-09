package com.example.distributed_systems.dto;

public class HeartBeatResponse {
   
    private boolean acknowledged;
    private String message;
    // Parameterized constructor

    public HeartBeatResponse(){
        
    }
    public HeartBeatResponse(boolean acknowledged, String message) {
        this.acknowledged = acknowledged;
        this.message = message;
    }
    // Getter for acknowledged
    public boolean isAcknowledged() {
        return acknowledged;
    }

    // Setter for acknowledged
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    // Getter for message
    public String getMessage() {
        return message;
    }

    // Setter for message
    public void setMessage(String message) {
        this.message = message;
    }
  
}
