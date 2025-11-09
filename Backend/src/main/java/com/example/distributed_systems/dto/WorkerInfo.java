
package com.example.distributed_systems.dto;
public class WorkerInfo {
    private String id;
   private String host;
    private int port;
    private boolean alive;
    private long lastHeartbeat;

     public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public WorkerInfo(String id, String host, int port, boolean alive,long lastHeartbeat) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.alive = alive;
        this.lastHeartbeat=lastHeartbeat;
    }
    public String getHost() {
        return host;
    }

    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public boolean isAlive() {
        return alive;
    }
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public void setId(String id) {
        this.id = id;
    }
}
