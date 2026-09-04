package io.quarkus.hibernate.orm.multitenancy.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;

/**
 * A custom {@link TenantConnectionResolver} using a non-{@link String} tenant identifier ({@link Long}).
 * <p>
 * All tenants map to the same datasource here; the test only needs to prove that this generic connection resolver is
 * discovered (by its concrete class) and invoked with a {@code Long} tenant identifier.
 */
@PersistenceUnitExtension
@ApplicationScoped
public class CustomLongConnectionResolver implements TenantConnectionResolver<Long> {

    @Inject
    @DataSource("tenant")
    AgroalDataSource dataSource;

    @Inject
    TenantAccessLog accessLog;

    @Override
    public ConnectionProvider resolve(Long tenantId) {
        accessLog.record(tenantId);
        return new QuarkusConnectionProvider(dataSource);
    }
}
