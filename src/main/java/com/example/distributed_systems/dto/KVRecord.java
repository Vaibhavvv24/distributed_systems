


public class KVRecord{

    private String key;
    private String value;
    private long timestamp;

    public KVRecord(String key, String value, long timestamp) {
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
    }
    public String getKey() {
            return key;
    }

    public String getValue() {
            return value;
    }

    public long getTimestamp() {
            return timestamp;
    }
    public void setValue(String value) {
            this.value = value;
    }
    public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
    }
    public void setKey(String key) {
            this.key = key;
    }

}