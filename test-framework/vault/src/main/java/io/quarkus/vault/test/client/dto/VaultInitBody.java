package io.quarkus.vault.test.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultInitBody implements VaultModel {

    public VaultInitBody(int secretShares, int secretThreshold) {
        this.secretShares = secretShares;
        this.secretThreshold = secretThreshold;
    }

    @JsonProperty("secret_shares")
    public int secretShares;
    @JsonProperty("secret_threshold")
    public int secretThreshold;

}
