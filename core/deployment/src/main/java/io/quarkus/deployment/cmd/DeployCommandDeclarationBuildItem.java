package io.quarkus.deployment.cmd;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Way for maven and gradle plugins to discover if any declared extensions
 * support quarkus deploy
 */
public final class DeployCommandDeclarationBuildItem extends MultiBuildItem {
    private final String name;

    public DeployCommandDeclarationBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
