package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKubernetesAuthBody implements VaultModel {

    public String role;
    public String jwt;

    public VaultKubernetesAuthBody(String role, String jwt) {
        this.role = role;
        this.jwt = jwt;
    }

}
