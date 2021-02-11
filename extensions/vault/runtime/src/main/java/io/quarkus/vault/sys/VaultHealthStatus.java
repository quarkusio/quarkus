package io.quarkus.vault.sys;

public class VaultHealthStatus {

    private boolean initialized;
    private boolean sealed;
    private boolean standby;
    private boolean performanceStandby;
    private String replicationPerfMode;
    private String replicationDrMode;
    private long serverTimeUtc;
    private String version;
    private String clusterName;
    private String clusterId;

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isSealed() {
        return sealed;
    }

    public void setSealed(boolean sealed) {
        this.sealed = sealed;
    }

    public boolean isStandby() {
        return standby;
    }

    public void setStandby(boolean standby) {
        this.standby = standby;
    }

    public boolean isPerformanceStandby() {
        return performanceStandby;
    }

    public void setPerformanceStandby(boolean performanceStandby) {
        this.performanceStandby = performanceStandby;
    }

    public String getReplicationPerfMode() {
        return replicationPerfMode;
    }

    public void setReplicationPerfMode(String replicationPerfMode) {
        this.replicationPerfMode = replicationPerfMode;
    }

    public String getReplicationDrMode() {
        return replicationDrMode;
    }

    public void setReplicationDrMode(String replicationDrMode) {
        this.replicationDrMode = replicationDrMode;
    }

    public long getServerTimeUtc() {
        return serverTimeUtc;
    }

    public void setServerTimeUtc(long serverTimeUtc) {
        this.serverTimeUtc = serverTimeUtc;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public String toString() {
        return "VaultHealth{initialized: " + initialized + ", sealed: " + sealed + '}';
    }
}
