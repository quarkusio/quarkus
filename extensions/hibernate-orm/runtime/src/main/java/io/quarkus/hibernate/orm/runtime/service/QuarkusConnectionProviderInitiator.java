package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.agroal.api.AgroalDataSource;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;

public final class QuarkusConnectionProviderInitiator implements StandardServiceInitiator<ConnectionProvider> {

    public static final QuarkusConnectionProviderInitiator INSTANCE = new QuarkusConnectionProviderInitiator();

    @Override
    public Class<ConnectionProvider> getServiceInitiated() {
        return ConnectionProvider.class;
    }

    @Override
    public ConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //First, check that this setup won't need to deal with multi-tenancy at the connection pool level:
        final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(configurationValues);
        if (strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA) {
            // nothing to do, but given the separate hierarchies have to handle this here.
            return null;
        }

        //Next, we'll want to try the Quarkus optimised pool:
        Object o = configurationValues.get(AvailableSettings.DATASOURCE);
        if (o != null) {
            final AgroalDataSource ds;
            try {
                ds = (AgroalDataSource) o;
            } catch (ClassCastException cce) {
                throw new HibernateException(
                        "A Datasource was configured as Connection Pool, but it's not the Agroal connection pool. In Quarkus, you need to use Agroal.");
            }
            return new QuarkusConnectionProvider(ds);
        }

        //When not using the Quarkus specific Datasource, delegate to traditional bootstrap so to not break
        //applications using persistence.xml :
        return ConnectionProviderInitiator.INSTANCE.initiateService(configurationValues, registry);
    }

}
