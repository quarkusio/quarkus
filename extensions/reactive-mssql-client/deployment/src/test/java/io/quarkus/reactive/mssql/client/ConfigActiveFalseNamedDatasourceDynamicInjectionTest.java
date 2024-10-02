package io.quarkus.reactive.mssql.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;

public class ConfigActiveFalseNamedDatasourceDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "mssql");

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<Pool> pool;

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<io.vertx.mutiny.sqlclient.Pool> mutinyPool;

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<MSSQLPool> vendorPool;

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<io.vertx.mutiny.mssqlclient.MSSQLPool> mutinyVendorPool;

    @Test
    public void pool() {
        doTest(pool, Pool::getConnection);
    }

    @Test
    public void mutinyPool() {
        doTest(mutinyPool, io.vertx.mutiny.sqlclient.Pool::getConnection);
    }

    @Test
    public void vendorPool() {
        doTest(vendorPool, Pool::getConnection);
    }

    @Test
    public void mutinyVendorPool() {
        doTest(mutinyVendorPool, io.vertx.mutiny.sqlclient.Pool::getConnection);
    }

    private <T> void doTest(InjectableInstance<T> instance, Consumer<T> action) {
        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean proxy cannot be null.
        var pool = instance.get();
        assertThat(pool).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> action.accept(pool))
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                + " to 'true' and configure datasource 'ds-1'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
