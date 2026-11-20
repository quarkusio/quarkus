package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

/**
 * The reactive data source completes on the Vert.x context of its caller, as the Vert.x Redis client does: a command
 * subscribed on a duplicated event loop context (the one of a request being served) emits on that same context, so
 * the continuation finds the request's context locals; a scoped connection block and a transaction do the same.
 * Without a context the continuation runs where Lettuce completed the command, on an event loop thread.
 */
class LettuceVertxContextTest extends CommandsTestBase {

    private final ReactiveRedisDataSource ds = reactiveDataSource();

    /** The context an item of the Uni {@code action} builds is emitted on, when subscribed on a fresh duplicated context. */
    private record Emission(Context subscribedOn, Context emittedOn, String thread) {
    }

    private static Emission subscribeOnDuplicatedContext(Function<Context, Uni<?>> action) throws Exception {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        assertThat(((ContextInternal) context).isDuplicate()).isTrue();
        assertThat(context.isEventLoopContext()).isTrue();
        CompletableFuture<Emission> emitted = new CompletableFuture<>();
        context.runOnContext(ignored -> action.apply(context).subscribe().with(
                item -> emitted.complete(new Emission(context, Vertx.currentContext(), Thread.currentThread().getName())),
                emitted::completeExceptionally));
        return emitted.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void commandCompletesOnTheCallersDuplicatedContext() throws Exception {
        rawSet(key, "hello");
        Emission emission = subscribeOnDuplicatedContext(context -> ds.value(String.class, String.class).get(key));
        assertThat(emission.emittedOn()).isSameAs(emission.subscribedOn());
    }

    @Test
    void scopedConnectionBlockRunsAndCompletesOnTheCallersDuplicatedContext() throws Exception {
        AtomicReference<Context> bodyContext = new AtomicReference<>();
        Emission emission = subscribeOnDuplicatedContext(context -> ds.withConnection(scoped -> {
            bodyContext.set(Vertx.currentContext());
            return scoped.value(String.class, String.class).set(key, "hello");
        }));
        assertThat(bodyContext.get()).isSameAs(emission.subscribedOn());
        assertThat(emission.emittedOn()).isSameAs(emission.subscribedOn());
    }

    @Test
    void scopedConnectionCompletesOnTheCallersDuplicatedContextAfterItsDatabaseIsReset() throws Exception {
        // The release puts the connection back on its default database before the outcome is emitted: a round trip
        // that completes on the event loop of the connection, not on the caller's context.
        Emission emission = subscribeOnDuplicatedContext(context -> ds.withConnection(scoped -> scoped.select(1)));
        assertThat(emission.emittedOn()).isSameAs(emission.subscribedOn());
    }

    @Test
    void transactionCompletesOnTheCallersDuplicatedContext() throws Exception {
        Emission emission = subscribeOnDuplicatedContext(
                context -> ds.withTransaction(tx -> tx.value(String.class, String.class).set(key, "hello")));
        assertThat(emission.emittedOn()).isSameAs(emission.subscribedOn());
        assertThat(rawGet(key)).isEqualTo("hello");
    }

    @Test
    void withoutACallerContextTheCommandCompletesWhereLettuceCompletedIt() throws Exception {
        rawSet(key, "hello");
        CompletableFuture<String> thread = new CompletableFuture<>();
        // subscribed from the test thread, which has no Vert.x context: the item is emitted by Lettuce's completion,
        // on the Vert.x event loop thread that serves the connection
        ds.value(String.class, String.class).get(key).subscribe().with(
                item -> thread.complete(Thread.currentThread().getName()), thread::completeExceptionally);
        assertThat(thread.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).contains("vert.x-eventloop");
    }
}
