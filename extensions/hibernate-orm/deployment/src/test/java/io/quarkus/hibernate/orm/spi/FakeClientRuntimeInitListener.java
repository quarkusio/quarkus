package io.quarkus.hibernate.orm.spi;

import java.util.function.BiConsumer;

import org.hibernate.cfg.AvailableSettings;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;

/**
 * A fake {@link HibernateOrmIntegrationRuntimeInitListener} that redirects to an Agroal datasource.
 * <p>
 * Used in tests to simulate an external client extension that manages
 * the database connection on behalf of the persistence unit.
 */
public class FakeClientRuntimeInitListener implements HibernateOrmIntegrationRuntimeInitListener {

    private String redirectDataSourceName;

    public FakeClientRuntimeInitListener() {
    }

    public String getRedirectDataSourceName() {
        return redirectDataSourceName;
    }

    public void setRedirectDataSourceName(String redirectDataSourceName) {
        this.redirectDataSourceName = redirectDataSourceName;
    }

    @Override
    public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
        AgroalDataSource ds = ClientProxy.unwrap(AgroalDataSourceUtil.dataSourceInstance(redirectDataSourceName).get());
        propertyCollector.accept(AvailableSettings.DATASOURCE, ds);
    }
}
