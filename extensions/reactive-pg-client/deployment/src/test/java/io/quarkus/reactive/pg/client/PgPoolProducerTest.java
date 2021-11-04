package io.quarkus.reactive.pg.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.pgclient.PgPool;

public class PgPoolProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-default-datasource.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanUsingBarePgClient.class)
                    .addClass(BeanUsingMutinyPgClient.class));

    @Inject
    BeanUsingBarePgClient beanUsingBare;

    @Inject
    BeanUsingMutinyPgClient beanUsingMutiny;

    @Test
    public void testVertxInjection() {
        beanUsingBare.verify()
                .thenCompose(v -> beanUsingMutiny.verify())
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
}
