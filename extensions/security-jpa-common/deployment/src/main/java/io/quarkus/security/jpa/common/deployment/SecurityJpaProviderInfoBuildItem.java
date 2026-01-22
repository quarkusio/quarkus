package io.quarkus.security.jpa.common.deployment;

import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Registers {@link io.quarkus.security.jpa.SecurityJpa} CDI bean producer class and generated identity provider names.
 */
public final class SecurityJpaProviderInfoBuildItem extends SimpleBuildItem {

    final Class<?> securityJpaProviderClass;
    final String jpaIdentityProviderImplName;
    final String jpaTrustedIdentityProviderImplName;
    final Class<?> jpaIdentityProviderClass;
    final Class<?> jpaTrustedIdentityProviderClass;

    public SecurityJpaProviderInfoBuildItem(Class<?> securityJpaProviderClass, String jpaIdentityProviderImplName,
            String jpaTrustedIdentityProviderImplName, Class<?> jpaIdentityProviderClass,
            Class<?> jpaTrustedIdentityProviderClass) {
        this.securityJpaProviderClass = Objects.requireNonNull(securityJpaProviderClass);
        this.jpaIdentityProviderImplName = jpaIdentityProviderImplName;
        this.jpaTrustedIdentityProviderImplName = jpaTrustedIdentityProviderImplName;
        this.jpaIdentityProviderClass = jpaIdentityProviderClass;
        this.jpaTrustedIdentityProviderClass = jpaTrustedIdentityProviderClass;
    }
}
