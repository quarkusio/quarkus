package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;

/**
 * Responsible for initializing the QuarkusRuntimeProxyFactoryFactory.
 * N.B. : this is a stateful Service Initiator, it carries the proxy definitions which have been
 * generated during the Hibernate ORM metadata analysis.
 */
public final class QuarkusRuntimeProxyFactoryFactoryInitiator implements StandardServiceInitiator<ProxyFactoryFactory> {

    private final ProxyDefinitions proxyClassDefinitions;

    public QuarkusRuntimeProxyFactoryFactoryInitiator(RecordedState rs) {
        proxyClassDefinitions = rs.getProxyClassDefinitions();
    }

    @Override
    public ProxyFactoryFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusRuntimeProxyFactoryFactory(proxyClassDefinitions);
    }

    @Override
    public Class<ProxyFactoryFactory> getServiceInitiated() {
        return ProxyFactoryFactory.class;
    }
}
