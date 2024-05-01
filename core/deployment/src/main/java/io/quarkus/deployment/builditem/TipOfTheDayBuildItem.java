package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

public final class TipOfTheDayBuildItem extends MultiBuildItem {
    private final String message;

    public TipOfTheDayBuildItem(final String message) {
        Assert.checkNotNullParam("message", message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
