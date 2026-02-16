package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType;

/**
 * The default value of the jar type depends on some external factors such as if AOT is enabled.
 * <p>
 * This build item provides the effective jar type, once these factors have been accounted for.
 */
public final class EffectiveJarTypeBuildItem extends SimpleBuildItem {

    private final JarType jarType;

    public EffectiveJarTypeBuildItem(JarType jarType) {
        this.jarType = jarType;
    }

    public JarType getJarType() {
        return jarType;
    }
}
