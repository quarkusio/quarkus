package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultUnwrapData implements VaultModel {

    private String token;

    public VaultUnwrapData(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
