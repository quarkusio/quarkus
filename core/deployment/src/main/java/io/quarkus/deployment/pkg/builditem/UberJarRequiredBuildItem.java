package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * marker build item that extensions can use to force uber jar creation
 */
public final class UberJarRequiredBuildItem extends MultiBuildItem {
}
