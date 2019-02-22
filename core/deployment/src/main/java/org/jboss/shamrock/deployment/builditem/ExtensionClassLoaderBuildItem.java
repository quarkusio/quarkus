package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

/**
 * The extension class loader.
 */
public final class ExtensionClassLoaderBuildItem extends SimpleBuildItem {
    private final ClassLoader extensionClassLoader;

    public ExtensionClassLoaderBuildItem(final ClassLoader extensionClassLoader) {
        this.extensionClassLoader = extensionClassLoader;
    }

    public ClassLoader getExtensionClassLoader() {
        return extensionClassLoader;
    }
}
