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
            .withConfigurationResource("application-default-datasource.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingBareMySQLClient.class)
                    .addClasses(BeanUsingMutinyMySQLClient.class)
                    .addClasses(BeanUsingAxleMySQLClient.class)
                    .addClasses(BeanUsingRXMySQLClient.class));

    @Inject
    BeanUsingBareMySQLClient beanUsingBare;

    @Inject
    BeanUsingAxleMySQLClient beanUsingAxle;

    @Inject
    BeanUsingRXMySQLClient beanUsingRx;

    @Inject
    BeanUsingMutinyMySQLClient beanUsingMutiny;

    @Test
    public void testVertxInjection() {
        beanUsingBare.verify()
                .thenCompose(v -> beanUsingAxle.verify())
                .thenCompose(v -> beanUsingRx.verify())
                .thenCompose(v -> beanUsingMutiny.verify())
                .toCompletableFuture()
                .join();
    }

    @ApplicationScoped
    static class BeanUsingBareMySQLClient {

        @Inject
        MySQLPool mysqlClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            mysqlClient.query("SELECT 1").execute(ar -> cf.complete(null));
            return cf;
        }
    }

    @ApplicationScoped
    static class BeanUsingAxleMySQLClient {

        @Inject
        io.vertx.mutiny.mysqlclient.MySQLPool mysqlClient;

        public CompletionStage<Void> verify() {
            return mysqlClient.query("SELECT 1").execute()
                    .onItem().ignore().andContinueWithNull()
                    .onFailure().recoverWithItem((Void) null)
                    .subscribeAsCompletionStage();
        }
    }

    @ApplicationScoped
    static class BeanUsingMutinyMySQLClient {

        @Inject
        io.vertx.axle.mysqlclient.MySQLPool mysqlClient;

        public CompletionStage<Void> verify() {
            return mysqlClient.query("SELECT 1").execute()
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
            mysqlClient.query("SELECT 1").rxExecute()
                    .ignoreElement()
                    .onErrorComplete()
                    .subscribe(() -> cf.complete(null));
            return cf;
        }
    }
}
