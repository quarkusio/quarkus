package io.quarkus.hibernate.rx.runtime.customized;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.rx.service.initiator.RxConnectionPoolProvider;
import org.hibernate.rx.service.initiator.RxConnectionProviderInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.vertx.sqlclient.Pool;

public final class QuarkusRxConnectionProviderInitiator implements StandardServiceInitiator<RxConnectionPoolProvider> {

    public static final QuarkusRxConnectionProviderInitiator INSTANCE = new QuarkusRxConnectionProviderInitiator();

    @Override
    public Class<RxConnectionPoolProvider> getServiceInitiated() {
        return RxConnectionPoolProvider.class;
    }

    @Override
    public RxConnectionPoolProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //First, check that this setup won't need to deal with multi-tenancy at the connection pool level:
        final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(configurationValues);
        if (strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA) {
            // nothing to do, but given the separate hierarchies have to handle this here.
            return null;
        }

        // Use the Quarkus-configured pool if available
        Object o = configurationValues.get(AvailableRxSettings.VERTX_POOL);
        if (o != null) {
            final Pool vertxPool;
            try {
                vertxPool = (Pool) o;
            } catch (ClassCastException cce) {
                throw new HibernateException("A Vertx Pool was configured as Connection Pool, but it's not an instance of a " +
                        Pool.class.getCanonicalName(), cce);
            }
            return new QuarkusRxConnectionPoolProvider(vertxPool);
        }
        //When not using the Quarkus specific ConnectionPool, delegate to traditional bootstrap so to not break
        //applications using persistence.xml :
        return RxConnectionProviderInitiator.INSTANCE.initiateService(configurationValues, registry);
    }

}
