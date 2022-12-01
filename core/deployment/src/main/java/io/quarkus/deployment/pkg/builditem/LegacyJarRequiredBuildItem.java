package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * marker build item that extensions can use to force legacy jar creation
 */
public final class LegacyJarRequiredBuildItem extends MultiBuildItem {
}
