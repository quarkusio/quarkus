package io.quarkus.vault.sys;

import java.util.List;

public class VaultInit {

    private List<String> keys;
    private List<String> keysBase64;
    private String rootToken;

    public VaultInit(List<String> keys, List<String> keysBase64, String rootToken) {
        this.keys = keys;
        this.keysBase64 = keysBase64;
        this.rootToken = rootToken;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<String> getKeysBase64() {
        return keysBase64;
    }

    public String getRootToken() {
        return rootToken;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VaultInit{");
        sb.append(keys.size());
        sb.append(" keys}");
        return sb.toString();
    }
}
