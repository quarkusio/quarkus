package io.quarkus.hibernate.orm.runtime.service.propertyaccessor;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusPropertyAccessorServiceInitiator implements StandardServiceInitiator<PropertyAccessorService> {

    public static final QuarkusPropertyAccessorServiceInitiator INSTANCE = new QuarkusPropertyAccessorServiceInitiator();

    private QuarkusPropertyAccessorServiceInitiator() {
    }

    @Override
    public PropertyAccessorService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return QuarkusPropertyAccessorService.INSTANCE;
    }

    @Override
    public Class<PropertyAccessorService> getServiceInitiated() {
        return PropertyAccessorService.class;
    }
}
