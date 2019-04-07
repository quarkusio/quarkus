package io.quarkus.vault.runtime.client.dto;

public class VaultKubernetesAuthBody implements VaultModel {

    public String role;
    public String jwt;

    public VaultKubernetesAuthBody(String role, String jwt) {
        this.role = role;
        this.jwt = jwt;
    }

}
