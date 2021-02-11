package io.quarkus.vault.runtime.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WrapInfo implements VaultModel {

    public String token;
    public long ttl;

    @JsonProperty("creation_time")
    public String creationTime;

    @JsonProperty("creation_path")
    public String creationPath;

}
