package io.quarkus.vault.test.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultHealth implements VaultModel {

    public boolean initialized;
    public boolean sealed;
    public boolean standby;
    @JsonProperty("performance_standby")
    public boolean performanceStandby;
    @JsonProperty("replicationPerfMode")
    public String replicationPerfMode;
    @JsonProperty("replication_dr_mode")
    public String replicationDrMode;
    @JsonProperty("server_time_utc")
    public long serverTimeUtc;
    public String version;
    @JsonProperty("cluster_name")
    public String clusterName;
    @JsonProperty("cluster_id")
    public String clusterId;

    @Override
    public String toString() {
        return "VaultHealth{initialized: " + initialized + ", sealed: " + sealed + '}';
    }

}
