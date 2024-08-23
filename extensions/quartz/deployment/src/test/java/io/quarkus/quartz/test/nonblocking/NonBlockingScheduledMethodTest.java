package io.quarkus.quartz.test.nonblocking;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class NonBlockingScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class, JobWasExecuted.class));

    @Test
    public void testVoid() throws InterruptedException {
        assertTrue(Jobs.VOID_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.VOID_ON_EVENT_LOOP.get());
        assertTrue(Jobs.SUCCESS_LATCH.await(5, TimeUnit.SECONDS));
        assertEvents("every_void");
    }

    @Test
    public void testUni() throws InterruptedException {
        assertTrue(Jobs.UNI_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.UNI_ON_EVENT_LOOP.get());
        assertTrue(Jobs.SUCCESS_LATCH.await(5, TimeUnit.SECONDS));
        assertEvents("every_uni");
    }

    @Test
    public void testCompletionStage() throws InterruptedException {
        assertTrue(Jobs.CS_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.CS_ON_EVENT_LOOP.get());
        assertTrue(Jobs.SUCCESS_LATCH.await(5, TimeUnit.SECONDS));
        assertEvents("every_cs");
    }

    private void assertEvents(String id) {
        for (SuccessfulExecution exec : Jobs.events) {
            if (exec.getExecution().getTrigger().getId().equals(id)) {
                return;
            }
        }
        fail("No SuccessfulExecution event fired for " + id + ": " + Jobs.events);
    }

    static class Jobs {

        // jobs executed
        static final CountDownLatch VOID_LATCH = new CountDownLatch(1);
        static final CountDownLatch UNI_LATCH = new CountDownLatch(1);
        static final CountDownLatch CS_LATCH = new CountDownLatch(1);

        // jobs executed on the event loop
        static final AtomicBoolean VOID_ON_EVENT_LOOP = new AtomicBoolean();
        static final AtomicBoolean UNI_ON_EVENT_LOOP = new AtomicBoolean();
        static final AtomicBoolean CS_ON_EVENT_LOOP = new AtomicBoolean();

        // successful events
        static final CountDownLatch SUCCESS_LATCH = new CountDownLatch(3);
        static final List<SuccessfulExecution> events = new CopyOnWriteArrayList<>();

        static void onSuccess(@Observes SuccessfulExecution event) {
            events.add(event);
            SUCCESS_LATCH.countDown();
        }

        @NonBlocking
        @Scheduled(every = "0.5s", identity = "every_void", skipExecutionIf = JobWasExecuted.class)
        void everySecond() {
            VOID_ON_EVENT_LOOP.set(Context.isOnEventLoopThread() && VertxContext.isOnDuplicatedContext());
            VOID_LATCH.countDown();
        }

        @Scheduled(every = "0.5s", identity = "every_uni", skipExecutionIf = JobWasExecuted.class)
        Uni<Void> everySecondUni() {
            UNI_ON_EVENT_LOOP.set(Context.isOnEventLoopThread() && VertxContext.isOnDuplicatedContext());
            UNI_LATCH.countDown();
            return Uni.createFrom().voidItem();
        }

        @Scheduled(every = "0.5s", identity = "every_cs", skipExecutionIf = JobWasExecuted.class)
        CompletionStage<Void> everySecondCompletionStage() {
            CompletableFuture<Void> ret = new CompletableFuture<Void>();
            CS_ON_EVENT_LOOP.set(Context.isOnEventLoopThread() && VertxContext.isOnDuplicatedContext());
            CS_LATCH.countDown();
            ret.complete(null);
            return ret;
        }
    }

    @Singleton
    static class JobWasExecuted implements Scheduled.SkipPredicate {

        @Override
        public boolean test(ScheduledExecution execution) {
            switch (execution.getTrigger().getId()) {
                case "every_void":
                    return Jobs.VOID_LATCH.getCount() == 0;
                case "every_uni":
                    return Jobs.UNI_LATCH.getCount() == 0;
                case "every_cs":
                    return Jobs.CS_LATCH.getCount() == 0;
                default:
                    return false;
            }
        }

    }

}
