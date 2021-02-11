package io.quarkus.hibernate.orm.deployment.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;

public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private HibernateOrmIntegrationRuntimeInitListener initListener;

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem(String integrationName, String persistenceUnitName) {
        if (integrationName == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.integrationName = integrationName;
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
    }

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem setInitListener(
            HibernateOrmIntegrationRuntimeInitListener initListener) {
        this.initListener = initListener;
        return this;
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationRuntimeConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    private HibernateOrmIntegrationRuntimeDescriptor toDescriptor() {
        return new HibernateOrmIntegrationRuntimeDescriptor(integrationName, Optional.ofNullable(initListener));
    }

    public static Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> collectDescriptors(
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> items) {
        Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> result = new HashMap<>();
        for (HibernateOrmIntegrationRuntimeConfiguredBuildItem item : items) {
            result.computeIfAbsent(item.persistenceUnitName, ignored -> new ArrayList<>())
                    .add(item.toDescriptor());
        }
        return result;
    }
}
