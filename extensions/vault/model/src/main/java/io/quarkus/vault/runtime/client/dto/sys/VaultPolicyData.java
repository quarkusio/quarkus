package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPolicyData implements VaultModel {

    public String name;
    public String rules;
}
