package io.quarkus.hibernate.orm.deployment.integration;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item which can be used by extensions to order the Hibernate ORM runtime startup.
 * Such {@link io.quarkus.builder.BuildStep build steps} producing this build item will
 * have their actions executed before Hibernate ORM runtime startup is triggered. This is
 * typically useful for extensions which want do certain actions (like setting up the DB
 * schema) that are necessary to happen before the Hibernate ORM runtime starts.
 */
public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String name;

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(HibernateOrmIntegrationRuntimeConfiguredBuildItem.class.getSimpleName())
                .append(" [").append(name).append("]")
                .toString();
    }
}
