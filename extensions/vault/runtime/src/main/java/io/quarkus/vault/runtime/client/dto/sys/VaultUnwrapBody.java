package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultUnwrapBody implements VaultModel {

    private String token;

    public VaultUnwrapBody(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

}
