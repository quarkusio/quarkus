package io.quarkus.hibernate.orm.runtime.service.propertyaccessor;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.property.access.spi.PropertyAccessorService;

final class QuarkusPropertyAccessorService implements PropertyAccessorService {

    static final QuarkusPropertyAccessorService INSTANCE = new QuarkusPropertyAccessorService();

    private final AccessorFactory accessorFactory = AccessorFactory.reflection();

    private QuarkusPropertyAccessorService() {
    }

    @Override
    public AccessorFactory hibernateAccessorFactory() {
        return accessorFactory;
    }
}
