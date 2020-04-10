package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultUnwrapBody implements VaultModel {

    public static final VaultUnwrapBody EMPTY = new VaultUnwrapBody();

    private String token;

    private VaultUnwrapBody() {
        this(null);
    }

    public VaultUnwrapBody(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

}
