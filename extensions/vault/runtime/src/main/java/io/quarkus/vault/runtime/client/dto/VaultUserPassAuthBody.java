package io.quarkus.vault.runtime.client.dto;

public class VaultUserPassAuthBody implements VaultModel {

    public VaultUserPassAuthBody(String password) {
        this.password = password;
    }

    public String password;

}
