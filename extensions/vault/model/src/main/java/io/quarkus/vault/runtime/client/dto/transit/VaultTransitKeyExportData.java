package io.quarkus.vault.runtime.client.dto.transit;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitKeyExportData implements VaultModel {

    public String name;
    public Map<String, String> keys;
}
