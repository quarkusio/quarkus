package io.quarkus.vault.sys;

public class VaultSealStatus {

    private String type;
    private boolean initialized;
    private boolean sealed;
    private int t;
    private int n;
    private int progress;
    private String nonce;
    private String version;
    private boolean migration;
    private String clusterName;
    private String clusterId;
    private boolean recoverySeal;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isMigration() {
        return migration;
    }

    public void setMigration(boolean migration) {
        this.migration = migration;
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

    public boolean isRecoverySeal() {
        return recoverySeal;
    }

    public void setRecoverySeal(boolean recoverySeal) {
        this.recoverySeal = recoverySeal;
    }

    @Override
    public String toString() {
        return "VaultSealStatus{" +
                "type: '" + type + '\'' +
                ", sealed: " + sealed +
                '}';
    }

}
