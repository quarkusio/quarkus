package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is meant to be internal to {@link io.quarkus.deployment.pkg.steps.JarTreeShakeProcessor},
 * in a sense it's produced and consumed by it.
 * <p>
 * Declares a class that must be treated as a root for jar tree-shake reachability analysis.
 * Any class registered through this build item will not be removed by the tree-shaker,
 * and all classes reachable from it will also be preserved.
 */
public final class JarTreeShakeRootClassBuildItem extends MultiBuildItem {

    private final String className;

    public JarTreeShakeRootClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
