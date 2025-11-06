

public class WorkerInfo {
    private String id;
   private String host;
    private int port;
    private boolean alive;

    public WorkerInfo(String id, String host, int port, boolean alive) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.alive = alive;
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
