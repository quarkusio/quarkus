package io.quarkus.vault.test.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleSecretIdData implements VaultModel {

    @JsonProperty("secret_id")
    public String secretId;

}
