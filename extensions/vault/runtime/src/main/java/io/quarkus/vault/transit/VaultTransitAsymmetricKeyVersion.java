package io.quarkus.vault.transit;

public class VaultTransitAsymmetricKeyVersion extends VaultTransitKeyVersion {

    private String name;
    private String publicKey;

    public String getName() {
        return name;
    }

    public VaultTransitAsymmetricKeyVersion setName(String name) {
        this.name = name;
        return this;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public VaultTransitAsymmetricKeyVersion setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }
}
