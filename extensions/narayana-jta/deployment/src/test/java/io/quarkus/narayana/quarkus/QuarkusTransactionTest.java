package io.quarkus.narayana.quarkus;

import static io.quarkus.narayana.jta.QuarkusTransaction.beginOptions;
import static io.quarkus.narayana.jta.QuarkusTransaction.runOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;

import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.narayana.jta.RunOptions;
import io.quarkus.test.QuarkusUnitTest;

public class QuarkusTransactionTest {

    private static AtomicInteger counter = new AtomicInteger();

    @Inject
    TransactionManager transactionManager;

    @Inject
    ThreadContext threadContext;

    @Inject
    TransactionScopedTestBean testBean;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void testBeginRequestScopeNotActive() {
        Assertions.assertThrows(ContextNotActiveException.class, QuarkusTransaction::begin);
    }

    @Test
    @ActivateRequestContext
    public void testBeginCommit() {
        QuarkusTransaction.begin();
        var sync = register();
        QuarkusTransaction.commit();
        Assertions.assertEquals(Status.STATUS_COMMITTED, sync.completionStatus);
    }

    @Test
    @ActivateRequestContext
    public void testBeginRollback() {
        QuarkusTransaction.begin();
        var sync = register();
        QuarkusTransaction.rollback();
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.completionStatus);
    }

    @Test
    @ActivateRequestContext
    public void testBeginSetRollbackOnly() {
        QuarkusTransaction.begin();
        var sync = register();
        QuarkusTransaction.setRollbackOnly();
        Assertions.assertThrows(QuarkusTransactionException.class, QuarkusTransaction::commit);
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.completionStatus);
    }

    @Test
    @ActivateRequestContext
    public void testBeginTimeout() throws InterruptedException {
        QuarkusTransaction.begin(beginOptions().timeout(1));
        var sync = register();
        Thread.sleep(1200);
        Assertions.assertThrows(QuarkusTransactionException.class, QuarkusTransaction::commit);
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.completionStatus);
    }

    @Test
    @ActivateRequestContext
    public void testBeginSuspendExistingFalse() {
        QuarkusTransaction.begin();
        var sync = register();
        Assertions.assertThrows(QuarkusTransactionException.class, QuarkusTransaction::begin);
        Assertions.assertTrue(QuarkusTransaction.isActive());
        QuarkusTransaction.commit();
        Assertions.assertEquals(Status.STATUS_COMMITTED, sync.completionStatus);
    }

    @Test
    public void testBeginRollbackOnRequestScopeEnd() {
        var context = Arc.container().requestContext();
        context.activate();
        TestSync sync = null;
        try {
            QuarkusTransaction.begin();
            sync = register();
        } finally {
            context.terminate();
        }
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.completionStatus);
    }

    @Test
    public void testBeginCommitOnRequestScopeEnd() {
        var context = Arc.container().requestContext();
        context.activate();
        TestSync sync = null;
        try {
            QuarkusTransaction.begin(beginOptions().commitOnRequestScopeEnd());
            sync = register();
        } finally {
            context.terminate();
        }
        Assertions.assertEquals(Status.STATUS_COMMITTED, sync.completionStatus);
    }

    @Test
    public void testCallCommit() {
        Assertions.assertEquals(Status.STATUS_COMMITTED, QuarkusTransaction.call(this::register).completionStatus);
    }

    @Test
    public void testCallRollback() {
        AtomicReference<TestSync> sync = new AtomicReference<>();
        Assertions.assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.call(() -> {
            sync.set(register());
            QuarkusTransaction.rollback();
            return null;
        }));
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.get().completionStatus);
    }

    @Test
    public void testCallRollbackOnly() {
        AtomicReference<TestSync> sync = new AtomicReference<>();
        Assertions.assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.call(() -> {
            sync.set(register());
            QuarkusTransaction.setRollbackOnly();
            return null;
        }));
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.get().completionStatus);
    }

    @Test
    public void testCallTimeout() {
        AtomicReference<TestSync> sync = new AtomicReference<>();
        Assertions.assertThrows(QuarkusTransactionException.class,
                () -> QuarkusTransaction.call(runOptions().timeout(1), () -> {
                    sync.set(register());
                    Thread.sleep(1200);
                    return null;
                }));
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.get().completionStatus);
    }

    @Test
    public void testCallException() {
        AtomicReference<TestSync> sync = new AtomicReference<>();
        Assertions.assertThrows(IllegalArgumentException.class, () -> QuarkusTransaction.call(() -> {
            sync.set(register());
            throw new IllegalArgumentException("foo");
        }));
        Assertions.assertEquals(Status.STATUS_ROLLEDBACK, sync.get().completionStatus);
    }

    @Test
    public void testCallExceptionHandler() {
        AtomicReference<TestSync> sync = new AtomicReference<>();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> QuarkusTransaction.call(runOptions().exceptionHandler((e) -> RunOptions.ExceptionResult.COMMIT), () -> {
                    sync.set(register());
                    throw new IllegalArgumentException("foo");
                }));
        Assertions.assertEquals(Status.STATUS_COMMITTED, sync.get().completionStatus);
    }

    @Test
    public void testCallSuspendExisting() {
        QuarkusTransaction.call(runOptions().semantic(RunOptions.Semantic.SUSPEND_EXISTING), () -> {
            Assertions.assertFalse(QuarkusTransaction.isActive());
            return null;
        });
    }

    @Test
    @ActivateRequestContext
    public void testCallDisallowExisting() {
        RunOptions options = runOptions().semantic(RunOptions.Semantic.DISALLOW_EXISTING);
        Assertions.assertEquals(Status.STATUS_COMMITTED, QuarkusTransaction.call(options, this::register).completionStatus);
        QuarkusTransaction.begin();
        Assertions.assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.call(options, this::register));
    }

    @Test
    @ActivateRequestContext
    public void testCallRequiresNew() throws SystemException {
        RunOptions options = runOptions().semantic(RunOptions.Semantic.REQUIRE_NEW);
        QuarkusTransaction.begin();
        var tx = transactionManager.getTransaction();
        QuarkusTransaction.call(options, () -> {
            Assertions.assertTrue(QuarkusTransaction.isActive());
            if (tx == transactionManager.getTransaction()) {
                throw new RuntimeException("Running in same transaction");
            }
            return null;
        });
    }

    @Test
    @ActivateRequestContext
    public void testCallJoinExisting() throws SystemException {
        RunOptions options = runOptions().semantic(RunOptions.Semantic.JOIN_EXISTING);
        QuarkusTransaction.begin();
        var tx = transactionManager.getTransaction();
        QuarkusTransaction.call(options, () -> {
            Assertions.assertTrue(QuarkusTransaction.isActive());
            if (tx != transactionManager.getTransaction()) {
                throw new RuntimeException("Running in different transaction");
            }
            return null;
        });
    }

    @Test
    public void testConcurrentTransactionScopedBeanCreation() {
        // 1. A Transaction is activated in a parent thread.
        QuarkusTransaction.run(() -> {
            ExecutorService executor = Executors.newCachedThreadPool();
            try {
                // 2. The parent thread starts 2 child threads, and propagates the transaction.
                // 3. The child threads access a @TransactionScoped bean concurrently,
                //    which has resource intensive producer simulated by sleep.
                var f1 = executor.submit(threadContext.contextualRunnable(() -> testBean.doWork()));
                var f2 = executor.submit(threadContext.contextualRunnable(() -> testBean.doWork()));

                f1.get();
                f2.get();
            } catch (Throwable e) {
                throw new AssertionError("Should not have thrown", e);
            } finally {
                executor.shutdownNow();
            }
        });

        // 4. The race condition is handled correctly, the bean is only created once.
        Assertions.assertEquals(1, counter.get());
    }

    TestSync register() {
        TestSync t = new TestSync();
        try {
            transactionManager.getTransaction().registerSynchronization(t);
        } catch (RollbackException | SystemException e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    static class TestSync implements Synchronization {

        int completionStatus = -1;

        @Override
        public void beforeCompletion() {

        }

        @Override
        public void afterCompletion(int status) {
            this.completionStatus = status;
        }
    }

    static class TransactionScopedTestBean {
        public void doWork() {

        }
    }

    @Singleton
    static class TransactionScopedTestBeanCreator {

        @Produces
        @TransactionScoped
        TransactionScopedTestBean transactionScopedTestBean() {
            try {
                Thread.sleep(100);
            } catch (Exception e) {

            }
            counter.incrementAndGet();
            return new TransactionScopedTestBean();
        }
    }
}
