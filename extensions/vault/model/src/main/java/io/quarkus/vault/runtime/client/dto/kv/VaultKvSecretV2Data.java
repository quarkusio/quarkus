package io.quarkus.vault.runtime.client.dto.kv;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKvSecretV2Data implements VaultModel {

    public Map<String, String> data;
    public VaultKvSecretV2Metadata metadata;

}
