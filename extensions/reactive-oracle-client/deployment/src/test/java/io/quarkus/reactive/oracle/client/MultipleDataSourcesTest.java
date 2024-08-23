package io.quarkus.reactive.oracle.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.oracleclient.OraclePool;

public class MultipleDataSourcesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-multiple-datasources.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanUsingDefaultDataSource.class)
                    .addClass(BeanUsingHibernateDataSource.class));

    @Inject
    BeanUsingDefaultDataSource beanUsingDefaultDataSource;

    @Inject
    BeanUsingHibernateDataSource beanUsingHibernateDataSource;

    @Test
    public void testMultipleDataSources() {
        beanUsingDefaultDataSource.verify()
                .thenCompose(v -> beanUsingHibernateDataSource.verify())
                .toCompletableFuture()
                .join();
    }

    @ApplicationScoped
    static class BeanUsingDefaultDataSource {

        @Inject
        OraclePool oracleClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            oracleClient.query("SELECT 1 FROM DUAL").execute(ar -> {
                if (ar.failed()) {
                    cf.completeExceptionally(ar.cause());
                } else {
                    cf.complete(null);
                }
            });
            return cf;
        }
    }

    @ApplicationScoped
    static class BeanUsingHibernateDataSource {

        @Inject
        @ReactiveDataSource("hibernate")
        OraclePool oracleClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            oracleClient.query("SELECT 1 FROM DUAL").execute(ar -> {
                if (ar.failed()) {
                    cf.completeExceptionally(ar.cause());
                } else {
                    cf.complete(null);
                }
            });
            return cf;
        }
    }
}
