package io.quarkus.vertx.nonblocking;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class NonBlockingProducerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Alpha.class, Bravo.class));

    @Inject
    Instance<Uni<Bravo>> bravo;

    @Inject
    Instance<CompletionStage<Charlie>> charlie;

    @Test
    public void testProducer() throws InterruptedException, ExecutionException, TimeoutException {
        assertNotNull(bravo.get().await().atMost(Duration.ofSeconds(10)));
        assertNotNull(charlie.get().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }

    @Singleton
    static class Alpha {

        @Produces
        Uni<Bravo> produceBravo() {
            assertTrue(Context.isOnEventLoopThread());
            assertTrue(VertxContext.isOnDuplicatedContext());
            return Uni.createFrom().item(new Bravo());
        }

        @Produces
        CompletionStage<Charlie> produceCharlie() {
            assertTrue(Context.isOnEventLoopThread());
            assertTrue(VertxContext.isOnDuplicatedContext());
            return CompletableFuture.completedStage(new Charlie());
        }

    }

    static class Bravo {

    }

    static class Charlie {

    }

}
