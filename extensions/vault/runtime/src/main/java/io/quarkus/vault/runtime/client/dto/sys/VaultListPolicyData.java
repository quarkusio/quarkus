package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultListPolicyData implements VaultModel {

    public List<String> policies;
}
