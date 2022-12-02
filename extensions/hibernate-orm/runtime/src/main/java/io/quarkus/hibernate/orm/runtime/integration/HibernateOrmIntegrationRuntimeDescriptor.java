package io.quarkus.hibernate.orm.runtime.integration;

import java.util.Optional;

import io.quarkus.runtime.annotations.RecordableConstructor;

public final class HibernateOrmIntegrationRuntimeDescriptor {
    private final String integrationName;
    private final Optional<HibernateOrmIntegrationRuntimeInitListener> initListener;

    @RecordableConstructor
    public HibernateOrmIntegrationRuntimeDescriptor(String integrationName,
            Optional<HibernateOrmIntegrationRuntimeInitListener> initListener) {
        this.integrationName = integrationName;
        this.initListener = initListener;
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationRuntimeDescriptor.class.getSimpleName() + " [" + integrationName + "]";
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public Optional<HibernateOrmIntegrationRuntimeInitListener> getInitListener() {
        return initListener;
    }
}
