package io.quarkus.reactive.pg.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.pgclient.PgPool;

public class PgPoolProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-default-datasource.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingBarePgClient.class)
                    .addClass(BeanUsingMutinyPgClient.class)
                    .addClasses(BeanUsingAxlePgClient.class)
                    .addClasses(BeanUsingRXPgClient.class));

    @Inject
    BeanUsingBarePgClient beanUsingBare;

    @Inject
    BeanUsingAxlePgClient beanUsingAxle;

    @Inject
    BeanUsingRXPgClient beanUsingRx;

    @Inject
    BeanUsingMutinyPgClient beanUsingMutiny;

    @Test
    public void testVertxInjection() {
        beanUsingBare.verify()
                .thenCompose(v -> beanUsingMutiny.verify())
                .thenCompose(v -> beanUsingAxle.verify())
                .thenCompose(v -> beanUsingRx.verify())
                .toCompletableFuture()
                .join();
    }

    @ApplicationScoped
    static class BeanUsingBarePgClient {

        @Inject
        PgPool pgClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            pgClient.query("SELECT 1").execute(ar -> cf.complete(null));
            return cf;
        }
    }

    @ApplicationScoped
    static class BeanUsingMutinyPgClient {

        @Inject
        io.vertx.mutiny.pgclient.PgPool pgClient;

        public CompletionStage<Void> verify() {
            return pgClient.query("SELECT 1").execute()
                    .onItem().ignore().andContinueWithNull()
                    .onFailure().recoverWithItem(() -> null)
                    .subscribeAsCompletionStage();
        }
    }

    @ApplicationScoped
    static class BeanUsingAxlePgClient {

        @Inject
        io.vertx.axle.pgclient.PgPool pgClient;

        public CompletionStage<Void> verify() {
            return pgClient.query("SELECT 1").execute()
                    .<Void> thenApply(rs -> null)
                    .exceptionally(t -> null);
        }
    }

    @ApplicationScoped
    static class BeanUsingRXPgClient {

        @Inject
        io.vertx.reactivex.pgclient.PgPool pgClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            pgClient.query("SELECT 1").rxExecute()
                    .ignoreElement()
                    .onErrorComplete()
                    .subscribe(() -> cf.complete(null));
            return cf;
        }
    }
}
