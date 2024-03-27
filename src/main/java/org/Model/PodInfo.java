package org.Model;

public class PodInfo {
    private String id;
    private String name;
    private String ip;
    private String status;

    public PodInfo(){

    }
    public PodInfo(String id, String name, String ip, String status) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

