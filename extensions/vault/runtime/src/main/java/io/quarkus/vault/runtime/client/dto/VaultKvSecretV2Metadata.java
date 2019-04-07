package io.quarkus.vault.runtime.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultKvSecretV2Metadata implements VaultModel {

    @JsonProperty("created_time")
    public String createdTime;
    @JsonProperty("deletion_time")
    public String deletionTime;
    public boolean destroyed;
    public int version;

}
