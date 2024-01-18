package io.quarkus.reactive.mysql.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Pool;

/**
 * We should be able to start the application, even with no configuration at all.
 */
public class NoConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    private static final Duration MAX_WAIT = Duration.ofSeconds(10);

    @Inject
    MyBean myBean;

    @Test
    public void pool_default() {
        Pool pool = Arc.container().instance(Pool.class).get();

        // The default datasource is a bit special;
        // it's historically always been considered as "present" even if there was no explicit configuration.
        // So the bean will never be null.
        assertThat(pool).isNotNull();
        // However, if unconfigured, it will use default connection config (host, port, username, ...) and will fail.
        assertThat(pool.getConnection().toCompletionStage())
                .failsWithin(MAX_WAIT)
                .withThrowableThat()
                .withMessageContaining("Connection refused");
    }

    @Test
    public void mutinyPool_default() {
        io.vertx.mutiny.sqlclient.Pool pool = Arc.container().instance(io.vertx.mutiny.sqlclient.Pool.class).get();

        // The default datasource is a bit special;
        // it's historically always been considered as "present" even if there was no explicit configuration.
        // So the bean will never be null.
        assertThat(pool).isNotNull();
        // However, if unconfigured, it will use default connection config (host, port, username, ...) and will fail.
        assertThat(pool.getConnection().subscribeAsCompletionStage())
                .failsWithin(MAX_WAIT)
                .withThrowableThat()
                .withMessageContaining("Connection refused");
    }

    @Test
    public void vendorPool_default() {
        MySQLPool pool = Arc.container().instance(MySQLPool.class).get();

        // The default datasource is a bit special;
        // it's historically always been considered as "present" even if there was no explicit configuration.
        // So the bean will never be null.
        assertThat(pool).isNotNull();
        // However, if unconfigured, it will use default connection config (host, port, username, ...) and will fail.
        assertThat(pool.getConnection().toCompletionStage())
                .failsWithin(MAX_WAIT)
                .withThrowableThat()
                .withMessageContaining("Connection refused");
    }

    @Test
    public void mutinyVendorPool_default() {
        io.vertx.mutiny.mysqlclient.MySQLPool pool = Arc.container().instance(io.vertx.mutiny.mysqlclient.MySQLPool.class)
                .get();

        // The default datasource is a bit special;
        // it's historically always been considered as "present" even if there was no explicit configuration.
        // So the bean will never be null.
        assertThat(pool).isNotNull();
        // However, if unconfigured, it will use default connection config (host, port, username, ...) and will fail.
        assertThat(pool.getConnection().subscribeAsCompletionStage())
                .failsWithin(MAX_WAIT)
                .withThrowableThat()
                .withMessageContaining("Connection refused");
    }

    @Test
    public void pool_named() {
        Pool pool = Arc.container().instance(Pool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();
        // An unconfigured, named datasource has no corresponding bean.
        assertThat(pool).isNull();
    }

    @Test
    public void mutinyPool_named() {
        io.vertx.mutiny.sqlclient.Pool pool = Arc.container().instance(io.vertx.mutiny.sqlclient.Pool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();
        // An unconfigured, named datasource has no corresponding bean.
        assertThat(pool).isNull();
    }

    @Test
    public void vendorPool_named() {
        MySQLPool pool = Arc.container().instance(MySQLPool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();
        // An unconfigured, named datasource has no corresponding bean.
        assertThat(pool).isNull();
    }

    @Test
    public void mutinyVendorPool_named() {
        io.vertx.mutiny.mysqlclient.MySQLPool pool = Arc.container().instance(io.vertx.mutiny.mysqlclient.MySQLPool.class,
                new ReactiveDataSource.ReactiveDataSourceLiteral("ds-1")).get();
        // An unconfigured, named datasource has no corresponding bean.
        assertThat(pool).isNull();
    }

    @Test
    public void injectedBean_default() {
        assertThat(myBean.usePool())
                .failsWithin(MAX_WAIT)
                .withThrowableThat()
                .withMessageContaining("Connection refused");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        MySQLPool pool;

        public CompletionStage<?> usePool() {
            return pool.getConnection().toCompletionStage();
        }
    }
}
