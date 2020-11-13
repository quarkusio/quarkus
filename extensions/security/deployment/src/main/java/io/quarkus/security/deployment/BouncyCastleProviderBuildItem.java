package io.quarkus.security.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BouncyCastleProviderBuildItem extends SimpleBuildItem {
    private final boolean inFipsMode;

    public BouncyCastleProviderBuildItem() {
        this(false);
    }

    public BouncyCastleProviderBuildItem(boolean inFipsMode) {
        this.inFipsMode = inFipsMode;
    }

    public boolean isInFipsMode() {
        return inFipsMode;
    }
}
