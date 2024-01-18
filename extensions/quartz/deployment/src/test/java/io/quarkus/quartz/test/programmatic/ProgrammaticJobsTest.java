package io.quarkus.quartz.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Scheduler.JobDefinition;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class ProgrammaticJobsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Inject
    Scheduler scheduler;

    @Inject
    MyService myService;

    static final CountDownLatch SYNC_LATCH = new CountDownLatch(1);
    static final CountDownLatch SYNC_CLASS_LATCH = new CountDownLatch(1);
    static final CountDownLatch ASYNC_LATCH = new CountDownLatch(1);
    static final AtomicInteger SKIPPED_EXECUTIONS = new AtomicInteger();
    static final CountDownLatch ASYNC_CLASS_LATCH = new CountDownLatch(1);

    @Test
    public void testJobs() throws InterruptedException {
        scheduler.newJob("alwaysSkip1")
                .setInterval("1s")
                .setSkipPredicate(ex -> true)
                .setTask(ex -> SKIPPED_EXECUTIONS.incrementAndGet())
                .schedule();
        scheduler.newJob("alwaysSkip2")
                .setInterval("1s")
                .setTask(ex -> SKIPPED_EXECUTIONS.incrementAndGet())
                .setSkipPredicate(AlwaysSkipPredicate.class)
                .schedule();

        Scheduler.JobDefinition job1 = scheduler.newJob("foo")
                .setInterval("1s")
                .setTask(ec -> {
                    assertTrue(Arc.container().requestContext().isActive());
                    myService.countDown(SYNC_LATCH);
                });

        assertEquals("Sync task was already set",
                assertThrows(IllegalStateException.class, () -> job1.setAsyncTask(ec -> null)).getMessage());

        Scheduler.JobDefinition job2 = scheduler.newJob("foo").setCron("0/5 * * * * ?");
        assertEquals("Either sync or async task must be set",
                assertThrows(IllegalStateException.class, () -> job2.schedule()).getMessage());
        job2.setTask(ec -> {
        });

        Trigger trigger1 = job1.schedule();
        assertNotNull(trigger1);
        assertTrue(ProgrammaticJobsTest.SYNC_LATCH.await(5, TimeUnit.SECONDS));

        assertEquals("Cannot modify a job that was already scheduled",
                assertThrows(IllegalStateException.class, () -> job1.setCron("fff")).getMessage());

        // Since job1 was already scheduled - job2 defines a non-unique identity
        assertEquals("A job with this identity is already scheduled: foo",
                assertThrows(IllegalStateException.class, () -> job2.schedule()).getMessage());

        // Identity must be unique
        assertEquals("A job with this identity is already scheduled: foo",
                assertThrows(IllegalStateException.class, () -> scheduler.newJob("foo")).getMessage());
        assertEquals("A job with this identity is already scheduled: bar",
                assertThrows(IllegalStateException.class, () -> scheduler.newJob("bar")).getMessage());

        // No-op
        assertNull(scheduler.unscheduleJob("bar"));
        assertNull(scheduler.unscheduleJob("nonexisting"));

        assertNotNull(scheduler.unscheduleJob("foo"));
        assertNotNull(scheduler.unscheduleJob("alwaysSkip1"));
        assertNotNull(scheduler.unscheduleJob("alwaysSkip2"));
        assertEquals(0, SKIPPED_EXECUTIONS.get());
        // Jobs#dummy()
        assertEquals(1, scheduler.getScheduledJobs().size());
    }

    @Test
    public void testAsyncJob() throws InterruptedException {
        JobDefinition asyncJob = scheduler.newJob("fooAsync")
                .setInterval("1s")
                .setAsyncTask(ec -> {
                    assertTrue(Context.isOnEventLoopThread() && VertxContext.isOnDuplicatedContext());
                    assertTrue(Arc.container().requestContext().isActive());
                    myService.countDown(ASYNC_LATCH);
                    return Uni.createFrom().voidItem();
                });

        assertEquals("Async task was already set",
                assertThrows(IllegalStateException.class, () -> asyncJob.setTask(ec -> {
                })).getMessage());

        Trigger trigger = asyncJob.schedule();
        assertNotNull(trigger);

        assertTrue(ProgrammaticJobsTest.ASYNC_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(scheduler.unscheduleJob("fooAsync"));
    }

    @Test
    public void testClassJobs() throws InterruptedException {
        scheduler.newJob("fooClass")
                .setInterval("1s")
                .setTask(JobClassTask.class)
                .schedule();
        assertTrue(ProgrammaticJobsTest.SYNC_CLASS_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(scheduler.unscheduleJob("fooClass"));
    }

    @Test
    public void testClassAsyncJobs() throws InterruptedException {
        scheduler.newJob("fooAsyncClass")
                .setInterval("1s")
                .setAsyncTask(JobClassAsyncTask.class)
                .schedule();
        assertTrue(ProgrammaticJobsTest.ASYNC_CLASS_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(scheduler.unscheduleJob("fooAsyncClass"));
    }

    static class Jobs {

        @Scheduled(identity = "bar", every = "60m")
        static void dummy() {
        }
    }

    @RequestScoped
    static class MyService {

        void countDown(CountDownLatch latch) {
            latch.countDown();
        }

    }

    public static class AlwaysSkipPredicate implements Scheduled.SkipPredicate {

        @Override
        public boolean test(ScheduledExecution execution) {
            return true;

        }
    }

    @Unremovable
    @Singleton
    public static class JobClassTask implements Consumer<ScheduledExecution> {

        @Inject
        MyService myService;

        @Override
        public void accept(ScheduledExecution se) {
            assertTrue(Arc.container().requestContext().isActive());
            myService.countDown(SYNC_CLASS_LATCH);
        }

    }

    @Unremovable
    @Singleton
    public static class JobClassAsyncTask implements Function<ScheduledExecution, Uni<Void>> {

        @Inject
        MyService myService;

        @Override
        public Uni<Void> apply(ScheduledExecution se) {
            assertTrue(Context.isOnEventLoopThread() && VertxContext.isOnDuplicatedContext());
            assertTrue(Arc.container().requestContext().isActive());
            myService.countDown(ASYNC_CLASS_LATCH);
            return Uni.createFrom().voidItem();
        }

    }

}
