package io.quarkus.vault.runtime.client.dto.kv;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKvListSecretsData implements VaultModel {

    public List<String> keys;

}
