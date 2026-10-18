package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.reactive.provider.service.NoJdbcConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class QuarkusNoJdbcConnectionProviderInitiator implements StandardServiceInitiator<ConnectionProvider> {
    public static final QuarkusNoJdbcConnectionProviderInitiator INSTANCE = new QuarkusNoJdbcConnectionProviderInitiator();

    private QuarkusNoJdbcConnectionProviderInitiator() {
    }

    @Override
    public ConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return NoJdbcConnectionProvider.INSTANCE;
    }

    @Override
    public Class<ConnectionProvider> getServiceInitiated() {
        return ConnectionProvider.class;
    }
}
