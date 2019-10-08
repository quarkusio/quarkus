package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A class loader that can be used to load deployment classes without causing
 * problems with transformers.
 */
public final class DeploymentClassLoaderBuildItem extends SimpleBuildItem {

    final ClassLoader classLoader;

    public DeploymentClassLoaderBuildItem(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
