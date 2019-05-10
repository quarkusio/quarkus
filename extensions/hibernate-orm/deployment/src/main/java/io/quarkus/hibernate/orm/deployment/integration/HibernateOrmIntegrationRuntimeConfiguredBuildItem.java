package io.quarkus.hibernate.orm.deployment.integration;

import io.quarkus.builder.item.MultiBuildItem;

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
