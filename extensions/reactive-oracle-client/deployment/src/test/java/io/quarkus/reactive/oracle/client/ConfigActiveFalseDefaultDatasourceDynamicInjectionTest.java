package io.quarkus.reactive.oracle.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.oracleclient.OraclePool;
import io.vertx.sqlclient.Pool;

public class ConfigActiveFalseDefaultDatasourceDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.active", "false");

    @Inject
    InjectableInstance<Pool> pool;

    @Inject
    InjectableInstance<io.vertx.mutiny.sqlclient.Pool> mutinyPool;

    @Inject
    InjectableInstance<OraclePool> vendorPool;

    @Inject
    InjectableInstance<io.vertx.mutiny.oracleclient.OraclePool> mutinyVendorPool;

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
                .hasMessageContainingAll("Datasource '<default>' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.active'"
                                + " to 'true' and configure datasource '<default>'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

}
