package io.quarkus.hibernate.orm.deployment.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;

public final class HibernateOrmIntegrationStaticConfiguredBuildItem extends MultiBuildItem {

    private final String name;
    private final String persistenceUnitName;
    private final HibernateOrmIntegrationStaticInitListener listener;

    public HibernateOrmIntegrationStaticConfiguredBuildItem(String name, String persistenceUnitName,
            HibernateOrmIntegrationStaticInitListener listener) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
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

    public static Map<String, List<HibernateOrmIntegrationStaticInitListener>> collectListeners(
            List<HibernateOrmIntegrationStaticConfiguredBuildItem> items) {
        Map<String, List<HibernateOrmIntegrationStaticInitListener>> listeners = new HashMap<>();
        for (HibernateOrmIntegrationStaticConfiguredBuildItem item : items) {
            listeners.computeIfAbsent(item.getPersistenceUnitName(), ignored -> new ArrayList<>())
                    .add(item.getListener());
        }
        return listeners;
    }
}
