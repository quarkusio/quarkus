package io.quarkus.reactive.mysql.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.sqlclient.Pool;

public class ConfigUrlMissingNamedDatasourceStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "mysql");

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        // When the URL is missing, the client assumes a default one.
        // See https://github.com/quarkusio/quarkus/issues/43517
        // In this case the default won't work, resulting in a connection exception.
        assertThatThrownBy(() -> myBean.usePool().toCompletableFuture().join())
                .cause()
                .isInstanceOf(ConnectException.class);
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        @ReactiveDataSource("ds-1")
        Pool pool;

        public CompletionStage<?> usePool() {
            return pool.getConnection().toCompletionStage().toCompletableFuture();
        }
    }
}
