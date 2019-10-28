package io.quarkus.vault.runtime.client.dto.transit;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitRandomData implements VaultModel {

    @JsonProperty("random_bytes")
    public String randomBytes;

}
