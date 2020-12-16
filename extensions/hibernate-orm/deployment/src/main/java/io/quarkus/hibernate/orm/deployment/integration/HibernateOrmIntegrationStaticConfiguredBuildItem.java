package io.quarkus.hibernate.orm.deployment.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;

public final class HibernateOrmIntegrationStaticConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private HibernateOrmIntegrationStaticInitListener initListener;
    private boolean xmlMappingRequired = false;

    public HibernateOrmIntegrationStaticConfiguredBuildItem(String integrationName, String persistenceUnitName) {
        if (integrationName == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.integrationName = integrationName;
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationStaticConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public HibernateOrmIntegrationStaticConfiguredBuildItem setInitListener(
            HibernateOrmIntegrationStaticInitListener initListener) {
        this.initListener = initListener;
        return this;
    }

    public HibernateOrmIntegrationStaticConfiguredBuildItem setXmlMappingRequired(boolean xmlMappingRequired) {
        this.xmlMappingRequired = xmlMappingRequired;
        return this;
    }

    private HibernateOrmIntegrationStaticDescriptor toDescriptor() {
        return new HibernateOrmIntegrationStaticDescriptor(integrationName, Optional.ofNullable(initListener),
                xmlMappingRequired);
    }

    public static Map<String, List<HibernateOrmIntegrationStaticDescriptor>> collectDescriptors(
            List<HibernateOrmIntegrationStaticConfiguredBuildItem> items) {
        Map<String, List<HibernateOrmIntegrationStaticDescriptor>> result = new HashMap<>();
        for (HibernateOrmIntegrationStaticConfiguredBuildItem item : items) {
            result.computeIfAbsent(item.persistenceUnitName, ignored -> new ArrayList<>())
                    .add(item.toDescriptor());
        }
        return result;
    }
}
