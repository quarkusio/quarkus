package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKubernetesAuthListRolesData implements VaultModel {

    public List<String> keys;
}
