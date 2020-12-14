package io.quarkus.hibernate.orm.runtime.cdi;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class QuarkusManagedBeanRegistryInitiator implements StandardServiceInitiator<ManagedBeanRegistry> {
    public static final QuarkusManagedBeanRegistryInitiator INSTANCE = new QuarkusManagedBeanRegistryInitiator();

    @Override
    public Class<ManagedBeanRegistry> getServiceInitiated() {
        return ManagedBeanRegistry.class;
    }

    @Override
    public ManagedBeanRegistry initiateService(Map configurationValues, ServiceRegistryImplementor serviceRegistry) {
        return new QuarkusManagedBeanRegistry();
    }
}
