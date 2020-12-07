package io.quarkus.hibernate.orm.deployment.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;

public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String name;
    private final String persistenceUnitName;
    private final HibernateOrmIntegrationRuntimeInitListener listener;

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem(String name,
            String persistenceUnitName, HibernateOrmIntegrationRuntimeInitListener listener) {
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

    public HibernateOrmIntegrationRuntimeInitListener getListener() {
        return listener;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(HibernateOrmIntegrationRuntimeConfiguredBuildItem.class.getSimpleName())
                .append(" [").append(name).append("]")
                .toString();
    }

    public static Map<String, List<HibernateOrmIntegrationRuntimeInitListener>> collectListeners(
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> items) {
        Map<String, List<HibernateOrmIntegrationRuntimeInitListener>> listeners = new HashMap<>();
        for (HibernateOrmIntegrationRuntimeConfiguredBuildItem item : items) {
            listeners.computeIfAbsent(item.getPersistenceUnitName(), ignored -> new ArrayList<>())
                    .add(item.getListener());
        }
        return listeners;
    }
}
