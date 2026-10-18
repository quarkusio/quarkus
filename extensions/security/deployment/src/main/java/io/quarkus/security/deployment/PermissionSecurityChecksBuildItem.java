package io.quarkus.security.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.security.deployment.PermissionSecurityChecks.PermissionSecurityChecksBuilder;

/**
 * Carries instance of the {@link PermissionSecurityChecksBuilder} that needs to be used by different build steps
 * inside Quarkus Security deployment module. This is internal build item only required within the security module.
 */
final class PermissionSecurityChecksBuilderBuildItem extends SimpleBuildItem {

    final PermissionSecurityChecksBuilder instance;

    PermissionSecurityChecksBuilderBuildItem(PermissionSecurityChecksBuilder builder) {
        this.instance = builder;
    }
}
