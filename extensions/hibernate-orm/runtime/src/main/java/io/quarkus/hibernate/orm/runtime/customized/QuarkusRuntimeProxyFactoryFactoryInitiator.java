package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Responsible for initializing the QuarkusRuntimeProxyFactoryFactory.
 * N.B. : this is a stateful Service Initiator, it carries the proxy definitions which have been
 * generated during the Hibernate ORM metadata analysis.
 */
public final class QuarkusRuntimeProxyFactoryFactoryInitiator implements StandardServiceInitiator<ProxyFactoryFactory> {

    private final QuarkusRuntimeProxyFactoryFactory proxyFactoryFactory;

    public QuarkusRuntimeProxyFactoryFactoryInitiator(QuarkusRuntimeProxyFactoryFactory proxyFactoryFactory) {
        this.proxyFactoryFactory = proxyFactoryFactory;
    }

    @Override
    public ProxyFactoryFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return proxyFactoryFactory;
    }

    @Override
    public Class<ProxyFactoryFactory> getServiceInitiated() {
        return ProxyFactoryFactory.class;
    }
}
