package io.quarkus.vault.runtime.client.dto.sys;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultLeasesBody implements VaultModel {

    @JsonProperty("lease_id")
    public String leaseId;

    public VaultLeasesBody(String leaseId) {
        this.leaseId = leaseId;
    }

}
