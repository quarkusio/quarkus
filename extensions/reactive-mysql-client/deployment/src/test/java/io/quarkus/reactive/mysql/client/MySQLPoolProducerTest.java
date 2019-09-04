package io.quarkus.reactive.mysql.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mysqlclient.MySQLPool;

public class MySQLPoolProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingBareMySQLClient.class)
                    .addClasses(BeanUsingAxleMySQLClient.class)
                    .addClasses(BeanUsingRXMySQLClient.class));

    @Inject
    BeanUsingBareMySQLClient beanUsingBare;

    @Inject
    BeanUsingAxleMySQLClient beanUsingAxle;

    @Inject
    BeanUsingRXMySQLClient beanUsingRx;

    @Test
    public void testVertxInjection() throws Exception {
        beanUsingBare.verify()
                .thenCompose(v -> beanUsingAxle.verify())
                .thenCompose(v -> beanUsingRx.verify())
                .toCompletableFuture()
                .join();
    }

    @ApplicationScoped
    static class BeanUsingBareMySQLClient {

        @Inject
        MySQLPool mysqlClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            mysqlClient.query("SELECT 1", ar -> {
                cf.complete(null);
            });
            return cf;
        }
    }

    @ApplicationScoped
    static class BeanUsingAxleMySQLClient {

        @Inject
        io.vertx.axle.mysqlclient.MySQLPool mysqlClient;

        public CompletionStage<Void> verify() {
            return mysqlClient.query("SELECT 1")
                    .<Void> thenApply(rs -> null)
                    .exceptionally(t -> null);
        }
    }

    @ApplicationScoped
    static class BeanUsingRXMySQLClient {

        @Inject
        io.vertx.reactivex.mysqlclient.MySQLPool mysqlClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            mysqlClient.rxQuery("SELECT 1")
                    .ignoreElement()
                    .onErrorComplete()
                    .subscribe(() -> cf.complete(null));
            return cf;
        }
    }
}
