package io.quarkus.extest.deployment;

import java.security.interfaces.DSAPublicKey;

import org.jboss.builder.item.SimpleBuildItem;

final public class PublicKeyBuildItem extends SimpleBuildItem {
    private DSAPublicKey publicKey;

    public PublicKeyBuildItem(DSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public DSAPublicKey getPublicKey() {
        return publicKey;
    }
}
