package io.quarkus.hibernate.orm.deployment.integration;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;

public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String name;
    private final HibernateOrmIntegrationRuntimeInitListener listener;

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem(String name,
            HibernateOrmIntegrationRuntimeInitListener listener) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    public HibernateOrmIntegrationRuntimeInitListener getListener() {
        return listener;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(HibernateOrmIntegrationRuntimeConfiguredBuildItem.class.getSimpleName())
                .append(" [").append(name).append("]")
                .toString();
    }

    public static List<HibernateOrmIntegrationRuntimeInitListener> collectListeners(
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> items) {
        List<HibernateOrmIntegrationRuntimeInitListener> listeners = new ArrayList<>();
        for (HibernateOrmIntegrationRuntimeConfiguredBuildItem item : items) {
            listeners.add(item.getListener());
        }
        return listeners;
    }
}
