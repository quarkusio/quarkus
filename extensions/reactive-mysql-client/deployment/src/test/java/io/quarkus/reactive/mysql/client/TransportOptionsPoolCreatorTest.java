package io.quarkus.reactive.mysql.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.net.NetClientOptions;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.sqlclient.Pool;

public class TransportOptionsPoolCreatorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConfiguredPoolCreator.class, DefaultPoolCreator.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "mysql")
            .overrideConfigKey("quarkus.datasource.reactive.url", "mysql://localhost:1/db")
            .overrideConfigKey("quarkus.datasource.reactive.tcp-keep-alive", "true")
            .overrideConfigKey("quarkus.datasource.reactive.tcp-keep-alive-idle", "30s")
            .overrideConfigKey("quarkus.datasource.reactive.tcp-keep-alive-interval", "5s")
            .overrideConfigKey("quarkus.datasource.reactive.tcp-keep-alive-count", "3")
            .overrideConfigKey("quarkus.datasource.reactive.socket-read-idle-timeout", "1m")
            .overrideConfigKey("quarkus.datasource.reactive.socket-write-idle-timeout", "1500ms")
            .overrideConfigKey("quarkus.datasource.\"unconfigured\".db-kind", "mysql")
            .overrideConfigKey("quarkus.datasource.\"unconfigured\".reactive.url", "mysql://localhost:1/db");

    @Inject
    Pool pool;

    @Inject
    @ReactiveDataSource("unconfigured")
    Pool unconfiguredPool;

    @Test
    public void configuredOptionsArePassedToPoolCreator() {
        pool.size(); // force the creation of the pool
        NetClientOptions options = ConfiguredPoolCreator.netClientOptions;

        assertThat(options).isNotNull();
        assertThat(options.isTcpKeepAlive()).isTrue();
        assertThat(options.getTcpKeepAliveIdleSeconds()).isEqualTo(30);
        assertThat(options.getTcpKeepAliveIntervalSeconds()).isEqualTo(5);
        assertThat(options.getTcpKeepAliveCount()).isEqualTo(3);
        assertThat(options.getIdleTimeoutUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        assertThat(options.getReadIdleTimeout()).isEqualTo(60_000);
        assertThat(options.getWriteIdleTimeout()).isEqualTo(1_500);
        assertThat(options.getIdleTimeout()).isZero();
    }

    @Test
    public void unconfiguredOptionsKeepVertxDefaults() {
        unconfiguredPool.size(); // force the creation of the pool
        NetClientOptions options = DefaultPoolCreator.netClientOptions;
        NetClientOptions defaults = new NetClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.toJson()).isEqualTo(defaults.toJson());
    }

    @Singleton
    public static class ConfiguredPoolCreator implements PoolCreator {

        static volatile NetClientOptions netClientOptions;

        @Override
        public Pool create(Input input) {
            netClientOptions = input.netClientOptions();
            return MySQLBuilder.pool().with(input.poolOptions()).with(input.netClientOptions())
                    .connectingTo(input.connectOptionsList().get(0)).using(input.vertx()).build();
        }
    }

    @Singleton
    @ReactiveDataSource("unconfigured")
    public static class DefaultPoolCreator implements PoolCreator {

        static volatile NetClientOptions netClientOptions;

        @Override
        public Pool create(Input input) {
            netClientOptions = input.netClientOptions();
            return MySQLBuilder.pool().with(input.poolOptions()).with(input.netClientOptions())
                    .connectingTo(input.connectOptionsList().get(0)).using(input.vertx()).build();
        }
    }
}
