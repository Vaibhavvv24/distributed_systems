

public class WorkerRegisterRequest {
    private String id;
    private String address; // e.g. http://worker1:8081

    public WorkerRegisterRequest(String id, String address) {
        this.id = id;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setAddress(String address) {
        this.address = address;
    }

   
}
