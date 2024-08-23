package io.quarkus.security.jpa.common.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This build items holds {@link JpaSecurityDefinition} common for reactive and classic Jakarta Persistence Security.
 */
public final class JpaSecurityDefinitionBuildItem extends SimpleBuildItem {

    private final JpaSecurityDefinition jpaSecurityDefinition;

    JpaSecurityDefinitionBuildItem(JpaSecurityDefinition jpaSecurityDefinition) {
        this.jpaSecurityDefinition = jpaSecurityDefinition;
    }

    public JpaSecurityDefinition get() {
        return jpaSecurityDefinition;
    }
}
