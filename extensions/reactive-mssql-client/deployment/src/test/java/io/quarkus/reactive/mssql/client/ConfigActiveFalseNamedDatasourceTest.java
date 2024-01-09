package io.quarkus.reactive.mssql.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;

public class ConfigActiveFalseNamedDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "mssql");

    @Inject
    MyBean myBean;

    @Test
    public void pool() {
        Pool pool = Arc.container().instance(Pool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();

        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        assertThat(pool).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> pool.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                + " to 'true' and configure datasource 'ds-1'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    public void mutinyPool() {
        io.vertx.mutiny.sqlclient.Pool pool = Arc.container().instance(io.vertx.mutiny.sqlclient.Pool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();

        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        assertThat(pool).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> pool.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                + " to 'true' and configure datasource 'ds-1'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    public void vendorPool() {
        MSSQLPool pool = Arc.container().instance(MSSQLPool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();

        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        assertThat(pool).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> pool.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                + " to 'true' and configure datasource 'ds-1'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    public void mutinyVendorPool() {
        io.vertx.mutiny.mssqlclient.MSSQLPool pool = Arc.container().instance(io.vertx.mutiny.mssqlclient.MSSQLPool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();

        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        assertThat(pool).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> pool.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                + " to 'true' and configure datasource 'ds-1'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    public void injectedBean() {
        assertThatThrownBy(() -> myBean.usePool())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                + " to 'true' and configure datasource 'ds-1'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        @ReactiveDataSource("ds-1")
        Pool pool;

        public CompletionStage<?> usePool() {
            return pool.getConnection().toCompletionStage();
        }
    }
}
