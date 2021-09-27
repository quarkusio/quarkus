package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIRolesListData implements VaultModel {
    public List<String> keys;
}
