package io.quarkus.smallrye.health.runtime.providedchecks;

public class ProvidedHealthChecksSupport {

    private Integer threadMax;

    private Double heapMemoryMaxPercentage;

    private Double nonHeapMemoryMaxPercentage;

    private String inetAddressHost;

    private String socketHost;

    private Integer socketPort;

    private Double systemLoadMax;

    private String urlAddress;

    public Integer getThreadMax() {
        return threadMax;
    }

    public Double getHeapMemoryMaxPercentage() {
        return heapMemoryMaxPercentage;
    }

    public Double getNonHeapMemoryMaxPercentage() {
        return nonHeapMemoryMaxPercentage;
    }

    public String getInetAddressHost() {
        return inetAddressHost;
    }

    public String getSocketHost() {
        return socketHost;
    }

    public Integer getSocketPort() {
        return socketPort;
    }

    public Double getSystemLoadMax() {
        return systemLoadMax;
    }

    public String getUrlAddress() {
        return urlAddress;
    }

    public void setInetAddressHost(String inetAddressHost) {
        this.inetAddressHost = inetAddressHost;
    }

    public void setThreadMax(Integer threadMax) {
        this.threadMax = threadMax;
    }

    public void setHeapMemoryMaxPercentage(Double heapMemoryMaxPercentage) {
        this.heapMemoryMaxPercentage = heapMemoryMaxPercentage;
    }

    public void setNonHeapMemoryMaxPercentage(Double nonHeapMemoryMaxPercentage) {
        this.nonHeapMemoryMaxPercentage = nonHeapMemoryMaxPercentage;
    }

    public void setSocketHost(String socketHost) {
        this.socketHost = socketHost;
    }

    public void setSocketPort(Integer socketPort) {
        this.socketPort = socketPort;
    }

    public void setSystemLoadMax(Double systemLoadMax) {
        this.systemLoadMax = systemLoadMax;
    }

    public void setUrlAddress(String address) {
        this.urlAddress = address;
    }

}
