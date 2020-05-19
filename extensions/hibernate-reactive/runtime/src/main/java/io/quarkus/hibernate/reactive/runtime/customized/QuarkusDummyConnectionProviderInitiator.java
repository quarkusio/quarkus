package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.reactive.service.ReactiveDummyConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProviderInitiator;

public class QuarkusDummyConnectionProviderInitiator implements StandardServiceInitiator<ConnectionProvider> {

    public static final QuarkusDummyConnectionProviderInitiator INSTANCE = new QuarkusDummyConnectionProviderInitiator();

    @Override
    public Class<ConnectionProvider> getServiceInitiated() {
        return ConnectionProvider.class;
    }

    @Override
    public ConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        ConnectionProvider cp = QuarkusConnectionProviderInitiator.INSTANCE.initiateService(configurationValues, registry);
        return new ReactiveDummyConnectionProvider(cp);
    }

}
