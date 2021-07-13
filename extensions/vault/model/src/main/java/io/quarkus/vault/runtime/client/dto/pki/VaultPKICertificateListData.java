package io.quarkus.vault.runtime.client.dto.pki;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKICertificateListData implements VaultModel {
    public List<String> keys;
}
