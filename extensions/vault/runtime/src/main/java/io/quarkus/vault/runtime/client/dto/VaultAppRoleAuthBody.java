package io.quarkus.vault.runtime.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultAppRoleAuthBody {

    @JsonProperty("role_id")
    public String roleId;
    @JsonProperty("secret_id")
    public String secretId;

    public VaultAppRoleAuthBody(String roleId, String secretId) {
        this.roleId = roleId;
        this.secretId = secretId;
    }
}
