package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultUserPassAuthBody implements VaultModel {

    public VaultUserPassAuthBody(String password) {
        this.password = password;
    }

    public String password;

}
