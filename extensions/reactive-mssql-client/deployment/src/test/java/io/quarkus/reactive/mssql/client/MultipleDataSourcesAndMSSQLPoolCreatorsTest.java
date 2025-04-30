package io.quarkus.reactive.mssql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.sqlclient.Pool;

public class MultipleDataSourcesAndMSSQLPoolCreatorsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-multiple-datasources-with-erroneous-url.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanUsingDefaultDataSource.class)
                    .addClass(BeanUsingHibernateDataSource.class)
                    .addClass(DefaultMSSQLPoolCreator.class)
                    .addClass(HibernateMSSQLPoolCreator.class));

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
        Pool mSSQLClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            mSSQLClient.query("SELECT 1").execute(ar -> {
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
        Pool mSSQLClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            mSSQLClient.query("SELECT 1").execute(ar -> {
                if (ar.failed()) {
                    cf.completeExceptionally(ar.cause());
                } else {
                    cf.complete(null);
                }
            });
            return cf;
        }
    }

    @Singleton
    public static class DefaultMSSQLPoolCreator implements MSSQLPoolCreator {

        @Override
        public Pool create(Input input) {
            assertEquals(12345, input.msSQLConnectOptions().getPort()); // validate that the bean has been called for the proper datasource
            return Pool.pool(input.vertx(), input.msSQLConnectOptions().setHost("localhost").setPort(1435),
                    input.poolOptions());
        }
    }

    @Singleton
    @ReactiveDataSource("hibernate")
    public static class HibernateMSSQLPoolCreator implements MSSQLPoolCreator {

        @Override
        public Pool create(Input input) {
            assertEquals(55555, input.msSQLConnectOptions().getPort()); // validate that the bean has been called for the proper datasource
            return Pool.pool(input.vertx(), input.msSQLConnectOptions().setHost("localhost").setPort(1435),
                    input.poolOptions());
        }
    }
}
