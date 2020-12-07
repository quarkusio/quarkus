package io.quarkus.hibernate.orm.deployment.integration;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;

public final class HibernateOrmIntegrationStaticConfiguredBuildItem extends MultiBuildItem {

    private final String name;
    private final HibernateOrmIntegrationStaticInitListener listener;

    public HibernateOrmIntegrationStaticConfiguredBuildItem(String name, HibernateOrmIntegrationStaticInitListener listener) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    public HibernateOrmIntegrationStaticInitListener getListener() {
        return listener;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(HibernateOrmIntegrationStaticConfiguredBuildItem.class.getSimpleName())
                .append(" [").append(name).append("]")
                .toString();
    }

    public static List<HibernateOrmIntegrationStaticInitListener> collectListeners(
            List<HibernateOrmIntegrationStaticConfiguredBuildItem> items) {
        List<HibernateOrmIntegrationStaticInitListener> listeners = new ArrayList<>();
        for (HibernateOrmIntegrationStaticConfiguredBuildItem item : items) {
            listeners.add(item.getListener());
        }
        return listeners;
    }
}
