package io.quarkus.vault.runtime.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultLeasesBody implements VaultModel {

    @JsonProperty("lease_id")
    public String leaseId;

    public VaultLeasesBody(String leaseId) {
        this.leaseId = leaseId;
    }

}
