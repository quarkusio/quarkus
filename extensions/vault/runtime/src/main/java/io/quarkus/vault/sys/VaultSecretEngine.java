package io.quarkus.vault.sys;

public enum VaultSecretEngine {
    KEY_VALUE("kv"),
    KEY_VALUE_2("kv-v2"),
    DATABASE("database"),
    PKI("pki"),
    TOTP("totp"),
    TRANSIT("transit"),
    ;

    private final String type;

    VaultSecretEngine(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
