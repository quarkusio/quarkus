package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;

public final class QuarkusRuntimeProxyFactoryFactory implements StandardServiceInitiator<ProxyFactoryFactory> {

    private final ProxyDefinitions proxyClassDefinitions;

    public QuarkusRuntimeProxyFactoryFactory(RecordedState rs) {
        proxyClassDefinitions = rs.getProxyClassDefinitions();
    }

    @Override
    public ProxyFactoryFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusProxyFactoryFactory(proxyClassDefinitions);
    }

    @Override
    public Class<ProxyFactoryFactory> getServiceInitiated() {
        return ProxyFactoryFactory.class;
    }
}
