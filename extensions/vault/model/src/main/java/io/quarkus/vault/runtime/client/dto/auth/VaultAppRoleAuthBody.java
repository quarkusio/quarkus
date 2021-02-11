package io.quarkus.vault.runtime.client.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthBody implements VaultModel {

    @JsonProperty("role_id")
    public String roleId;
    @JsonProperty("secret_id")
    public String secretId;

    public VaultAppRoleAuthBody(String roleId, String secretId) {
        this.roleId = roleId;
        this.secretId = secretId;
    }
}
