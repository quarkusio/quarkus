package io.quarkus.reactive.mssql.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;

public class ConfigUrlMissingDefaultDatasourceDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    InjectableInstance<Pool> pool;

    @Inject
    InjectableInstance<io.vertx.mutiny.sqlclient.Pool> mutinyPool;

    @Inject
    InjectableInstance<MSSQLPool> vendorPool;

    @Inject
    InjectableInstance<io.vertx.mutiny.mssqlclient.MSSQLPool> mutinyVendorPool;

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
        doTest(vendorPool, MSSQLPool -> MSSQLPool.getConnection().toCompletionStage().toCompletableFuture().join());
    }

    @Test
    public void mutinyVendorPool() {
        doTest(mutinyVendorPool, MSSQLPool -> MSSQLPool.getConnection().subscribe().asCompletionStage().join());
    }

    private <T> void doTest(InjectableInstance<T> instance, Consumer<T> action) {
        var pool = instance.get();
        assertThat(pool).isNotNull();
        // When the URL is missing, the client assumes a default one.
        // See https://github.com/quarkusio/quarkus/issues/43517
        // In this case the default won't work, resulting in a connection exception.
        assertThatThrownBy(() -> action.accept(pool))
                .cause()
                .isInstanceOf(ConnectException.class);
    }
}
