package io.quarkus.vault.runtime.client.dto;

import java.util.Map;

public class VaultKvSecretV2Data implements VaultModel {

    public Map<String, String> data;
    public VaultKvSecretV2Metadata metadata;

}
