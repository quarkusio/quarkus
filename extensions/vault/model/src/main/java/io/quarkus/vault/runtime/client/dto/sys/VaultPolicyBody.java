package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPolicyBody implements VaultModel {

    public String policy;

    public VaultPolicyBody(String policy) {
        this.policy = policy;
    }
}
