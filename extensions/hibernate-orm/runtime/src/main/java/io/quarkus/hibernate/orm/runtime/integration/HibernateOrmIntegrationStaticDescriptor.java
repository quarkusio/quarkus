package io.quarkus.hibernate.orm.runtime.integration;

import java.util.Optional;

import io.quarkus.runtime.annotations.RecordableConstructor;

public final class HibernateOrmIntegrationStaticDescriptor {
    private final String integrationName;
    private final Optional<HibernateOrmIntegrationStaticInitListener> initListener;
    private final boolean xmlMappingRequired;

    @RecordableConstructor
    public HibernateOrmIntegrationStaticDescriptor(String integrationName,
            Optional<HibernateOrmIntegrationStaticInitListener> initListener, boolean xmlMappingRequired) {
        this.integrationName = integrationName;
        this.initListener = initListener;
        this.xmlMappingRequired = xmlMappingRequired;
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationStaticDescriptor.class.getSimpleName() + " [" + integrationName + "]";
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public Optional<HibernateOrmIntegrationStaticInitListener> getInitListener() {
        return initListener;
    }

    public boolean isXmlMappingRequired() {
        return xmlMappingRequired;
    }
}
