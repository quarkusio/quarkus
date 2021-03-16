package io.quarkus.vault.runtime.client.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleGenerateNewSecretIDData implements VaultModel {

    @JsonProperty("secret_id")
    public String secretId;

    @JsonProperty("secret_id_accessor")
    public String secretIdAccessor;

}
