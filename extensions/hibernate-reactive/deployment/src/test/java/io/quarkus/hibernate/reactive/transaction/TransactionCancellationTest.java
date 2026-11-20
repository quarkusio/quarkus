package io.quarkus.hibernate.reactive.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.transaction.runtime.pool.TransactionalContextPool;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.sqlclient.Transaction;

class TransactionCancellationTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final String PROBE_KEY = "transaction-cancellation.probe";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Hero.class, Service.class, Probe.class));

    @Inject
    Pool pool;
    @Inject
    Service service;
    @Inject
    Probe probe;

    @Test
    @RunOnVertxContext
    void cancellationDuringRowLockWait(UniAsserter asserter) {
        asserter.execute(() -> runScenario(true));
    }

    @Test
    @RunOnVertxContext
    void connectedControl(UniAsserter asserter) {
        asserter.execute(() -> runScenario(false));
    }

    @Test
    @RunOnVertxContext
    void timeoutRecoveryCanStartAnotherTransaction(UniAsserter asserter) {
        asserter.assertEquals(() -> service.neverCompletes()
                .ifNoItem().after(Duration.ofMillis(50)).fail()
                .onFailure(TimeoutException.class).recoverWithUni(service::one), 1);
    }

    @Test
    @RunOnVertxContext
    void subscriptionsHaveIndependentCancellation(UniAsserter asserter) {
        asserter.execute(() -> {
            final Context firstContext = newContext();
            final Context secondContext = newContext();
            final Probe first = new Probe();
            final Probe second = new Probe();
            final Uni<Void> work = service.waitForCancellation();
            return runOnContext(firstContext, () -> {
                ContextLocals.put(PROBE_KEY, first);
                first.subscription = work.subscribe().with(first.completed::tryComplete, first.completed::tryFail);
            }).chain(() -> runOnContext(secondContext, () -> {
                ContextLocals.put(PROBE_KEY, second);
                second.subscription = work.subscribe().with(second.completed::tryComplete, second.completed::tryFail);
            })).chain(() -> runOnContext(firstContext, () -> first.subscription.cancel()))
                    .chain(() -> await(first.businessCancelled.future()))
                    .invoke(() -> assertThat(second.businessCancelled.future().isComplete()).isFalse())
                    .chain(() -> runOnContext(secondContext, () -> second.subscription.cancel()))
                    .chain(() -> await(second.businessCancelled.future()))
                    .ifNoItem().after(TIMEOUT).fail()
                    .eventually(() -> cancel(firstContext, first))
                    .eventually(() -> cancel(secondContext, second));
        });
    }

    private Uni<Void> runScenario(boolean cancel) {
        probe.reset();
        final Context businessContext = newContext();
        return pool.query("delete from hero where id in (1, 2)").execute()
                .chain(() -> pool.query("insert into hero(id, name) values (1, '0')").execute())
                .chain(() -> pool.withConnection(lockConnection -> lockConnection.begin().chain(
                        lockTransaction -> lockConnection.query("select pg_backend_pid()").execute().chain(lockBackend -> {
                            final int lockPid = lockBackend.iterator().next().getInteger(0);
                            return lockConnection.query("update hero set name = name where id = 1").execute()
                                    .chain(() -> runOnContext(businessContext, () -> {
                                        probe.subscription = service.write().subscribe().with(
                                                probe.completed::tryComplete, probe.completed::tryFail);
                                    }))
                                    .chain(() -> waitForLock(lockPid))
                                    .call(() -> cancel ? cancel(businessContext, probe) : Uni.createFrom().voidItem())
                                    .chain(lockTransaction::commit)
                                    .chain(() -> await(cancel ? probe.businessCancelled.future() : probe.completed.future()))
                                    .call(() -> await(probe.transaction.completion()).onFailure().recoverWithNull())
                                    .invoke(() -> assertThat(probe.markerAttempts).as("chained INSERT invocations")
                                            .isEqualTo(cancel ? 0 : 1))
                                    .chain(() -> pool.query("select (select name from hero where id = 1) as counter, "
                                            + "(select count(*) from hero where id = 2) as markers").execute())
                                    .invoke(rows -> {
                                        final var row = rows.iterator().next();
                                        assertThat(row.getString("counter")).isEqualTo(cancel ? "0" : "1");
                                        assertThat(row.getLong("markers")).isEqualTo(cancel ? 0L : 1L);
                                    }).replaceWithVoid();
                        }))))
                .ifNoItem().after(TIMEOUT).fail()
                .eventually(() -> cancel(businessContext, probe));
    }

    private Uni<Void> waitForLock(int lockPid) {
        return Uni.createFrom().deferred(() -> pool.preparedQuery("select count(*) from pg_stat_activity "
                + "where pid = $1 and wait_event_type = 'Lock' and $2 = any(pg_blocking_pids(pid))")
                .execute(Tuple.of(probe.backendPid, lockPid))
                .chain(rows -> rows.iterator().next().getLong(0) == 1L ? Uni.createFrom().voidItem()
                        : Uni.createFrom().failure(new NotReady())))
                .onFailure(NotReady.class).retry().withBackOff(Duration.ofMillis(10), Duration.ofMillis(100))
                .expireIn(TIMEOUT.toMillis());
    }

    private static Context newContext() {
        final Context context = VertxContext.createNewDuplicatedContext(Vertx.currentContext());
        VertxContextSafetyToggle.setContextSafe(context, true);
        return context;
    }

    private static Uni<Void> cancel(Context context, Probe state) {
        return runOnContext(context, () -> {
            if (state.subscription != null) {
                state.subscription.cancel();
            }
        });
    }

    private static Uni<Void> runOnContext(Context context, Runnable action) {
        return Uni.createFrom().voidItem().emitOn(task -> context.runOnContext(ignored -> task.run())).invoke(action);
    }

    private static <T> Uni<T> await(Future<T> future) {
        return Uni.createFrom().emitter(emitter -> future.onComplete(emitter::complete, emitter::fail));
    }

    private static class NotReady extends RuntimeException {
    }

    @Singleton
    public static class Probe {
        int backendPid;
        int markerAttempts;
        Transaction transaction;
        Cancellable subscription;
        Promise<Void> businessCancelled = Promise.promise();
        Promise<Void> completed = Promise.promise();

        void reset() {
            backendPid = 0;
            markerAttempts = 0;
            transaction = null;
            subscription = null;
            businessCancelled = Promise.promise();
            completed = Promise.promise();
        }
    }

    @Singleton
    public static class Service {
        @Inject
        Mutiny.Session session;
        @Inject
        Probe probe;

        @Transactional
        public Uni<Integer> neverCompletes() {
            return Uni.createFrom().nothing();
        }

        @Transactional
        public Uni<Integer> one() {
            return session.createNativeQuery("select 1", Integer.class).getSingleResult();
        }

        @Transactional
        public Uni<Void> waitForCancellation() {
            final Probe state = ContextLocals.<Probe> get(PROBE_KEY).orElseThrow();
            return Uni.createFrom().<Void> nothing()
                    .onCancellation().invoke(() -> state.businessCancelled.tryComplete());
        }

        @Transactional
        public Uni<Void> write() {
            return session.createNativeQuery("select pg_backend_pid()", Integer.class).getSingleResult()
                    .invoke(pid -> {
                        probe.backendPid = pid;
                        probe.transaction = TransactionalContextPool.getCurrentConnectionFromVertxContext().result()
                                .transaction();
                    })
                    .chain(() -> session.createMutationQuery("update Hero set name = '1' where id = 1").executeUpdate())
                    .chain(() -> {
                        probe.markerAttempts++;
                        return session.createNativeMutationQuery("insert into hero(id, name) values (2, 'marker')")
                                .executeUpdate();
                    })
                    .replaceWithVoid()
                    .onCancellation().invoke(() -> probe.businessCancelled.tryComplete());
        }
    }
}
