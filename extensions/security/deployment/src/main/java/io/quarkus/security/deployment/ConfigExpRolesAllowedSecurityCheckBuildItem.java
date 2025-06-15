package io.quarkus.security.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.security.runtime.interceptor.check.SupplierRolesAllowedCheck;

/**
 * Marker build item that is used to indicate that there are {@link SupplierRolesAllowedCheck}s whose roles contains
 * config expressions that should be resolved at runtime.
 */
public final class ConfigExpRolesAllowedSecurityCheckBuildItem extends SimpleBuildItem {

    ConfigExpRolesAllowedSecurityCheckBuildItem() {
    }
}
