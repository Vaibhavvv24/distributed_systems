
package com.example.distributed_systems.dto;

public class KVRecord{

    private String key;
    private String value;

    public KVRecord(String key, String value) {
            this.key = key;
            this.value = value;
    }
    public String getKey() {
            return key;
    }

    public String getValue() {
            return value;
    }

   
    public void setValue(String value) {
            this.value = value;
    }
    public void setKey(String key) {
            this.key = key;
    }

}