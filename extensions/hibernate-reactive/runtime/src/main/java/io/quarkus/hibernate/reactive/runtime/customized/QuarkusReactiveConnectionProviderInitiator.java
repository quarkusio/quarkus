package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.reactive.cfg.ReactiveSettings;
import org.hibernate.reactive.service.initiator.ReactiveConnectionPoolProvider;
import org.hibernate.reactive.service.initiator.ReactiveConnectionProviderInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.vertx.sqlclient.Pool;

public final class QuarkusReactiveConnectionProviderInitiator
        implements StandardServiceInitiator<ReactiveConnectionPoolProvider> {

    public static final QuarkusReactiveConnectionProviderInitiator INSTANCE = new QuarkusReactiveConnectionProviderInitiator();

    @Override
    public Class<ReactiveConnectionPoolProvider> getServiceInitiated() {
        return ReactiveConnectionPoolProvider.class;
    }

    @Override
    public ReactiveConnectionPoolProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //First, check that this setup won't need to deal with multi-tenancy at the connection pool level:
        final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(configurationValues);
        if (strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA) {
            // nothing to do, but given the separate hierarchies have to handle this here.
            return null;
        }

        // Use the Quarkus-configured pool if available
        Object o = configurationValues.get(ReactiveSettings.VERTX_POOL);
        if (o != null) {
            final Pool vertxPool;
            try {
                vertxPool = (Pool) o;
            } catch (ClassCastException cce) {
                throw new HibernateException("A Vertx Pool was configured as Connection Pool, but it's not an instance of a " +
                        Pool.class.getCanonicalName(), cce);
            }
            return new QuarkusReactiveConnectionPoolProvider(vertxPool);
        }
        //When not using the Quarkus specific ConnectionPool, delegate to traditional bootstrap so to not break
        //applications using persistence.xml :
        return ReactiveConnectionProviderInitiator.INSTANCE.initiateService(configurationValues, registry);
    }

}
