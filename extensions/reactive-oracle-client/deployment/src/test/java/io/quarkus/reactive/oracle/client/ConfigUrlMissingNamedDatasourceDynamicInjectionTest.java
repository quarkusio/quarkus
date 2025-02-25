package io.quarkus.reactive.oracle.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.oracleclient.OraclePool;
import io.vertx.sqlclient.Pool;

public class ConfigUrlMissingNamedDatasourceDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "oracle");

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<Pool> pool;

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<io.vertx.mutiny.sqlclient.Pool> mutinyPool;

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<OraclePool> vendorPool;

    @Inject
    @ReactiveDataSource("ds-1")
    InjectableInstance<io.vertx.mutiny.oracleclient.OraclePool> mutinyVendorPool;

    @Test
    public void pool() {
        doTest(pool, pool1 -> pool1.getConnection().toCompletionStage().toCompletableFuture().join());
    }

    @Test
    public void mutinyPool() {
        doTest(mutinyPool, pool1 -> pool1.getConnection().subscribe().asCompletionStage().join());
    }

    @Test
    public void vendorPool() {
        doTest(vendorPool, oraclePool -> oraclePool.getConnection().toCompletionStage().toCompletableFuture().join());
    }

    @Test
    public void mutinyVendorPool() {
        doTest(mutinyVendorPool, oraclePool -> oraclePool.getConnection().subscribe().asCompletionStage().join());
    }

    private <T> void doTest(InjectableInstance<T> instance, Consumer<T> action) {
        var pool = instance.get();
        assertThat(pool).isNotNull();
        assertThatThrownBy(() -> action.accept(pool))
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll("Datasource 'ds-1' was deactivated automatically because its URL is not set.",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.\"ds-1\".reactive.url'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
