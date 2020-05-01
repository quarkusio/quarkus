package io.quarkus.hibernate.rx.runtime.customized;

import java.util.Map;

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
        System.out.println("@AGG initiating RxConnectionPool svc with props: " + configurationValues);
        //First, check that this setup won't need to deal with multi-tenancy at the connection pool level:
        final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(configurationValues);
        if (strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA) {
            // nothing to do, but given the separate hierarchies have to handle this here.
            return null;
        }

        // Use the Quarkus-configured pool if available
        Object o = configurationValues.get(AvailableRxSettings.VERTX_POOL);
        System.out.println("@AGG quarkus pool is: " + o);
        if (o != null) {
            Pool vertxPool = (Pool) o;
            return new QuarkusRxConnectionPoolProvider(vertxPool);
        }

        //
        //        //Next, we'll want to try the Quarkus optimised pool:
        //        Object o = configurationValues.get(AvailableSettings.DATASOURCE);
        //        if (o != null) {
        //            final AgroalDataSource ds;
        //            try {
        //                ds = (AgroalDataSource) o;
        //            } catch (ClassCastException cce) {
        //                throw new HibernateException(
        //                        "A Datasource was configured as Connection Pool, but it's not the Agroal connection pool. In Quarkus, you need to use Agroal.");
        //            }
        //            return new QuarkusConnectionProvider(ds);
        //        }

        System.out.println("@AGG Vertx pool NOT found in config");

        //When not using the Quarkus specific Datasource, delegate to traditional bootstrap so to not break
        //applications using persistence.xml :
        return RxConnectionProviderInitiator.INSTANCE.initiateService(configurationValues, registry);
    }

}
