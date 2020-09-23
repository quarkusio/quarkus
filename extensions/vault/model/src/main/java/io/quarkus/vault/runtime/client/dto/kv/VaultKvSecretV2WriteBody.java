package io.quarkus.vault.runtime.client.dto.kv;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKvSecretV2WriteBody implements VaultModel {

    public Map<String, Integer> options;
    public Map<String, String> data;

}
