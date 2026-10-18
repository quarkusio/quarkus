package io.quarkus.security.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

public final class BouncyCastleProviderBuildItem extends MultiBuildItem {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BouncyCastleProviderBuildItem that = (BouncyCastleProviderBuildItem) o;
        return inFipsMode == that.inFipsMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inFipsMode);
    }
}
