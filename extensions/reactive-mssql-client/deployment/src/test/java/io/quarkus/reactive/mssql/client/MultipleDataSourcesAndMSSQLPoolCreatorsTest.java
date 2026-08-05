package io.quarkus.reactive.mssql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.Pool;

public class MultipleDataSourcesAndMSSQLPoolCreatorsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
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
            mSSQLClient.query("SELECT 1").execute().onComplete(ar -> {
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
            mSSQLClient.query("SELECT 1").execute().onComplete(ar -> {
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
    public static class DefaultMSSQLPoolCreator implements PoolCreator {

        @Override
        public Pool create(Input input) {
            MSSQLConnectOptions options = (MSSQLConnectOptions) input.connectOptionsList().get(0);
            assertEquals(12345, options.getPort()); // validate that the bean has been called for the proper datasource
            return Pool.pool(input.vertx(), options.setHost("localhost").setPort(1435),
                    input.poolOptions());
        }
    }

    @Singleton
    @ReactiveDataSource("hibernate")
    public static class HibernateMSSQLPoolCreator implements PoolCreator {

        @Override
        public Pool create(Input input) {
            MSSQLConnectOptions options = (MSSQLConnectOptions) input.connectOptionsList().get(0);
            assertEquals(55555, options.getPort()); // validate that the bean has been called for the proper datasource
            return Pool.pool(input.vertx(), options.setHost("localhost").setPort(1435),
                    input.poolOptions());
        }
    }
}
