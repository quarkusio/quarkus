package io.quarkus.hibernate.orm.deployment.integration;

import io.quarkus.builder.item.MultiBuildItem;

public final class HibernateOrmIntegrationBuildItem extends MultiBuildItem {

    private final String name;

    public HibernateOrmIntegrationBuildItem(String name) {
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
        return new StringBuilder().append(HibernateOrmIntegrationBuildItem.class.getSimpleName())
                .append(" [").append(name).append("]")
                .toString();
    }
}
