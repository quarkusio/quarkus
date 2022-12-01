package io.quarkus.deployment.console;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that signifies that the interactive console is ready.
 *
 * This will not always be present, as the console may not be installed
 */
public final class ConsoleInstalledBuildItem extends SimpleBuildItem {

    public static final ConsoleInstalledBuildItem INSTANCE = new ConsoleInstalledBuildItem();
}
