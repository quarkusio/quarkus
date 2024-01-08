package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * We should be able to start the application, even with no configuration at all.
 */
public class NoConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    MyBean myBean;

    @Test
    public void dataSource_default() {
        DataSource ds = Arc.container().instance(DataSource.class).get();

        // The default datasource is a bit special;
        // it's historically always been considered as "present" even if there was no explicit configuration.
        // So the bean will never be null.
        assertThat(ds).isNotNull();
        // However, if unconfigured, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> ds.getConnection())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.datasource.jdbc.url has not been defined");
    }

    @Test
    public void agroalDataSource_default() {
        AgroalDataSource ds = Arc.container().instance(AgroalDataSource.class).get();

        // The default datasource is a bit special;
        // it's historically always been considered as "present" even if there was no explicit configuration.
        // So the bean will never be null.
        assertThat(ds).isNotNull();
        // However, if unconfigured, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> ds.getConnection())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.datasource.jdbc.url has not been defined");
    }

    @Test
    public void dataSource_named() {
        DataSource ds = Arc.container().instance(DataSource.class,
                new io.quarkus.agroal.DataSource.DataSourceLiteral("ds-1")).get();
        // An unconfigured, named datasource has no corresponding bean.
        assertThat(ds).isNull();
    }

    @Test
    public void agroalDataSource_named() {
        AgroalDataSource ds = Arc.container().instance(AgroalDataSource.class,
                new io.quarkus.agroal.DataSource.DataSourceLiteral("ds-1")).get();
        // An unconfigured, named datasource has no corresponding bean.
        assertThat(ds).isNull();
    }

    @Test
    public void injectedBean_default() {
        assertThatThrownBy(() -> myBean.useDataSource())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.datasource.jdbc.url has not been defined");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        DataSource ds;

        public void useDataSource() throws SQLException {
            ds.getConnection();
        }
    }
}
