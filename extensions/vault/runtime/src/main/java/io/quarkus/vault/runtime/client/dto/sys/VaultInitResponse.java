package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultInitResponse implements VaultModel {

    public List<String> keys;
    @JsonProperty("keys_base64")
    public List<String> keysBase64;
    @JsonProperty("root_token")
    public String rootToken;

}
