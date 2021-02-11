package io.quarkus.vault.test.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleRoleIdData implements VaultModel {

    @JsonProperty("role_id")
    public String roleId;
}
