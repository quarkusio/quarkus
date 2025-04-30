package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initializes the connection provider during static init.
 * <p>
 * Since the database is not available during static init,
 * the connection provider is just a stub that will fail on connection retrieval.
 */
public final class QuarkusStaticInitConnectionProviderInitiator implements StandardServiceInitiator<ConnectionProvider> {

    public static final QuarkusStaticInitConnectionProviderInitiator INSTANCE = new QuarkusStaticInitConnectionProviderInitiator();

    @Override
    public Class<ConnectionProvider> getServiceInitiated() {
        return ConnectionProvider.class;
    }

    @Override
    public ConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusStaticInitConnectionProvider();
    }

}
