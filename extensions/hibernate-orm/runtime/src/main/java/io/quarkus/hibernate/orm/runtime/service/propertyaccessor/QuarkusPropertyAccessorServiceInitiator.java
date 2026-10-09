package io.quarkus.hibernate.orm.runtime.service.propertyaccessor;

import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusPropertyAccessorServiceInitiator implements StandardServiceInitiator<PropertyAccessorService> {

    public static final String ACCESSOR_FACTORY = "io.quarkus.hibernate.orm.accessor_factory";

    public static final QuarkusPropertyAccessorServiceInitiator INSTANCE = new QuarkusPropertyAccessorServiceInitiator();

    private QuarkusPropertyAccessorServiceInitiator() {
    }

    @Override
    public PropertyAccessorService initiateService(Map<String, Object> configurationValues,
            ServiceRegistryImplementor registry) {
        return new QuarkusPropertyAccessorService((AccessorFactory) configurationValues.get(ACCESSOR_FACTORY));
    }

    @Override
    public Class<PropertyAccessorService> getServiceInitiated() {
        return PropertyAccessorService.class;
    }
}
