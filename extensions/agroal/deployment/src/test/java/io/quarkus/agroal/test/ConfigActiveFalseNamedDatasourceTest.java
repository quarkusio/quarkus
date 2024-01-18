package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.CreationException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseNamedDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.users.active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.users.db-kind", "h2");

    @Inject
    MyBean myBean;

    @Test
    public void dataSource() {
        DataSource ds = Arc.container().instance(DataSource.class,
                new io.quarkus.agroal.DataSource.DataSourceLiteral("users")).get();

        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        assertThat(ds).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> ds.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'users' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"users\".active'"
                                + " to 'true' and configure datasource 'users'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    public void agroalDataSource() {
        DataSource ds = Arc.container().instance(DataSource.class,
                new io.quarkus.agroal.DataSource.DataSourceLiteral("users")).get();

        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        assertThat(ds).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> ds.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'users' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"users\".active'"
                                + " to 'true' and configure datasource 'users'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    public void injectedBean() {
        assertThatThrownBy(() -> myBean.useDatasource())
                .isInstanceOf(CreationException.class)
                .hasMessageContainingAll("Datasource 'users' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"users\".active'"
                                + " to 'true' and configure datasource 'users'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        @io.quarkus.agroal.DataSource("users")
        DataSource ds;

        public void useDatasource() throws SQLException {
            ds.getConnection();
        }
    }
}
