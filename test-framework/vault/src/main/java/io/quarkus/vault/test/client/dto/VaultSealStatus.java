package io.quarkus.vault.test.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

/**
 * {
 * "type":"shamir",
 * "initialized":true,
 * "sealed":false,
 * "t":1,
 * "n":1,
 * "progress":0,
 * "nonce":"",
 * "version":"1.2.2",
 * "migration":false,
 * "cluster_name":"vault-cluster-21617f37",
 * "cluster_id":"c47a6792-ec86-8e59-0747-5729f95db971",
 * "recovery_seal":false
 * }
 */
public class VaultSealStatus implements VaultModel {

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
                '}';
    }
}
