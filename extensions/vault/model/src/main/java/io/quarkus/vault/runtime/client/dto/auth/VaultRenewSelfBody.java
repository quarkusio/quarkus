package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultRenewSelfBody implements VaultModel {

    // eg: 1h
    public String increment;

    public VaultRenewSelfBody(String increment) {
        this.increment = increment;
    }

}
