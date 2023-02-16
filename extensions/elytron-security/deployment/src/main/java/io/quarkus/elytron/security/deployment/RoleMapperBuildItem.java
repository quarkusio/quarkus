package io.quarkus.elytron.security.deployment;

import org.wildfly.security.authz.RoleMapper;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * This represents a {@linkplain RoleMapper} instance output by a build step.
 */
public final class RoleMapperBuildItem extends SimpleBuildItem {

    private final RuntimeValue<RoleMapper> roleMapper;

    public RoleMapperBuildItem(RuntimeValue<RoleMapper> roleMapper) {
        this.roleMapper = roleMapper;
    }

    public RuntimeValue<RoleMapper> getRoleMapper() {
        return roleMapper;
    }
}
