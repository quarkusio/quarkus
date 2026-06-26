package io.quarkus.data.processor;

import org.hibernate.processor.spi.QuarkusDataTypeNames;

public class QuarkusDataHibernateTypeNames implements QuarkusDataTypeNames {

    @Override
    public String packageName() {
        return "io.quarkus.data.hibernate";
    }

    @Override
    public String entityMarker() {
        return "io.quarkus.data.hibernate.EntitySwitcher";
    }

    @Override
    public String managedBlockingRepositoryBase() {
        return "io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepositoryBase";
    }

    @Override
    public String statelessBlockingRepositoryBase() {
        return "io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingRepositoryBase";
    }

    @Override
    public String managedReactiveRepositoryBase() {
        return "io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveRepositoryBase";
    }

    @Override
    public String statelessReactiveRepositoryBase() {
        return "io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveRepositoryBase";
    }
}
