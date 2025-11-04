
import java.util.List;


public class RouteResponse {
    private String key;
    private String primary;
    private List<String> replicas;
    // getters/setters/constructors
    public RouteResponse(String key, String primary, List<String> replicas) {
        this.key = key;
        this.primary = primary;
        this.replicas = replicas;
    }

    public String getKey() {
        return key;
    }

    public String getPrimary() {
        return primary;
    }

    public List<String> getReplicas() {
        return replicas;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public void setReplicas(List<String> replicas) {
        this.replicas = replicas;
    }
}
