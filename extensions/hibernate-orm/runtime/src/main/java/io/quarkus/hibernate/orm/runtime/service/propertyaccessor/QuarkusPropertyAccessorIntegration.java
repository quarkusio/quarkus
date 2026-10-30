package io.quarkus.hibernate.orm.runtime.service.propertyaccessor;

import java.util.function.BiConsumer;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;

import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.runtime.RuntimeValue;

public final class QuarkusPropertyAccessorIntegration implements HibernateOrmIntegrationStaticInitListener {

    private final RuntimeValue<AccessorFactory> accessorFactory;

    public QuarkusPropertyAccessorIntegration(RuntimeValue<AccessorFactory> accessorFactory) {
        this.accessorFactory = accessorFactory;
    }

    @Override
    public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
        propertyCollector.accept(QuarkusPropertyAccessorServiceInitiator.ACCESSOR_FACTORY, accessorFactory.getValue());
    }

    @Override
    public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
            BiConsumer<String, Object> propertyCollector) {
        // Retain the factory when the service registry is rebuilt at runtime.
        contributeBootProperties(propertyCollector);
    }
}
