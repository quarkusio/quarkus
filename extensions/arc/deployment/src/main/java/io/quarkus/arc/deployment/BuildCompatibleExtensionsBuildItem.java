package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.bcextensions.ExtensionsEntryPoint;
import io.quarkus.builder.item.SimpleBuildItem;

public final class BuildCompatibleExtensionsBuildItem extends SimpleBuildItem {
    public final ExtensionsEntryPoint entrypoint = new ExtensionsEntryPoint();
}
