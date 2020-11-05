package io.quarkus.vault.runtime.client.dto.transit;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitListKeysData implements VaultModel {

    public List<String> keys;
}
