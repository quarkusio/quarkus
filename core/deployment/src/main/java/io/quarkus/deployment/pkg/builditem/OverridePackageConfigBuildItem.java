package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build steps that use {@link io.quarkus.deployment.pkg.PackageConfig} should
 * consume this build item to order themselves after build steps that modify PackageConfig
 *
 * Build steps that modify PackageConfig should produce this.
 */
public final class OverridePackageConfigBuildItem extends MultiBuildItem {
}
