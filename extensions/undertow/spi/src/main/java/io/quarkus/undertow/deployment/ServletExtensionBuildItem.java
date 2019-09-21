package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.undertow.servlet.ServletExtension;

public final class ServletExtensionBuildItem extends MultiBuildItem {

    private final ServletExtension value;

    public ServletExtensionBuildItem(ServletExtension value) {
        this.value = value;
    }

    public ServletExtension getValue() {
        return value;
    }
}
