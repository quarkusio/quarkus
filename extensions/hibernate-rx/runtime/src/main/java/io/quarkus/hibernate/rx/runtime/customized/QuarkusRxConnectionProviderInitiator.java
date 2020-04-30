package io.quarkus.hibernate.rx.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.rx.service.RxConnectionPoolProviderImpl;
import org.hibernate.rx.service.initiator.RxConnectionProviderInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusRxConnectionProviderInitiator implements StandardServiceInitiator<RxConnectionPoolProviderImpl> {

    public static final QuarkusRxConnectionProviderInitiator INSTANCE = new QuarkusRxConnectionProviderInitiator();

    @Override
    public Class<RxConnectionPoolProviderImpl> getServiceInitiated() {
        return RxConnectionPoolProviderImpl.class;
    }

    @Override
    public RxConnectionPoolProviderImpl initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //        //First, check that this setup won't need to deal with multi-tenancy at the connection pool level:
        //        final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(configurationValues);
        //        if (strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA) {
        //            // nothing to do, but given the separate hierarchies have to handle this here.
        //            return null;
        //        }
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

        //When not using the Quarkus specific Datasource, delegate to traditional bootstrap so to not break
        //applications using persistence.xml :
        System.out.println("@AGG initiating RxConnectionPool svc with props: " + configurationValues);
        return RxConnectionProviderInitiator.INSTANCE.initiateService(configurationValues, registry);
    }

}
