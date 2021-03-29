package io.quarkus.security.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BouncyCastleJsseProviderBuildItem extends SimpleBuildItem {
    private final boolean inFipsMode;

    public BouncyCastleJsseProviderBuildItem() {
        this(false);
    }

    public BouncyCastleJsseProviderBuildItem(boolean inFipsMode) {
        this.inFipsMode = inFipsMode;
    }

    public boolean isInFipsMode() {
        return inFipsMode;
    }

}
