package io.quarkus.reactive.mssql.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mssqlclient.MSSQLPool;

public class MSSQLPoolProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-default-datasource.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanUsingBareMSSQLClient.class)
                    .addClasses(BeanUsingMutinyMSSQLClient.class));

    @Inject
    BeanUsingBareMSSQLClient beanUsingBare;

    @Inject
    BeanUsingMutinyMSSQLClient beanUsingMutiny;

    @Test
    public void testVertxInjection() {
        beanUsingBare.verify()
                .thenCompose(v -> beanUsingMutiny.verify())
                .toCompletableFuture()
                .join();
    }

    @ApplicationScoped
    static class BeanUsingBareMSSQLClient {

        @Inject
        MSSQLPool mssqlClient;

        public CompletionStage<Void> verify() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            mssqlClient.query("SELECT 1").execute(ar -> cf.complete(null));
            return cf;
        }
    }

    @ApplicationScoped
    static class BeanUsingMutinyMSSQLClient {

        @Inject
        io.vertx.mutiny.mssqlclient.MSSQLPool mssqlClient;

        public CompletionStage<Void> verify() {
            return mssqlClient.query("SELECT 1").execute()
                    .onItem().ignore().andContinueWithNull()
                    .onFailure().recoverWithItem((Void) null)
                    .subscribeAsCompletionStage();
        }
    }
}
