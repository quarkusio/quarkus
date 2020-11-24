package io.quarkus.vault.transit;

import java.util.Map;

public class VaultTransitKeyExportDetail {

    private String name;
    private Map<String, String> keys;

    public String getName() {
        return name;
    }

    public VaultTransitKeyExportDetail setName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public VaultTransitKeyExportDetail setKeys(Map<String, String> keys) {
        this.keys = keys;
        return this;
    }
}
