package io.quarkus.vault.runtime.client.dto;

public class VaultRenewSelfBody implements VaultModel {

    // eg: 1h
    public String increment;

    public VaultRenewSelfBody(String increment) {
        this.increment = increment;
    }

}
