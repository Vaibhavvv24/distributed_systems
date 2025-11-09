package com.example.distributed_systems.dto;

public class PutResponse {
    private String key;
    private boolean success;
    private String message;


    public PutResponse(String key, boolean success, String message) {
        this.key = key;
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
}

