package io.quarkus.vault.runtime.client.dto.sys;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultSealStatusResult implements VaultModel {

    public String type;
    public boolean initialized;
    public boolean sealed;
    public int t;
    public int n;
    public int progress;
    public String nonce;
    public String version;
    public boolean migration;
    @JsonProperty("cluster_name")
    public String clusterName;
    @JsonProperty("cluster_id")
    public String clusterId;
    @JsonProperty("recovery_seal")
    public boolean recoverySeal;

    @Override
    public String toString() {
        return "VaultSealStatus{" +
                "type: '" + type + '\'' +
                ", sealed: " + sealed +
                ", initialized: " + initialized +
                '}';
    }
}
