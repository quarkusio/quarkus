package io.quarkus.reactive.mssql.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mssqlclient.MSSQLPool;

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
        MSSQLPool msSQLClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            msSQLClient.query("SELECT 1").execute(ar -> {
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
        MSSQLPool msSQLClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            msSQLClient.query("SELECT 1").execute(ar -> {
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
