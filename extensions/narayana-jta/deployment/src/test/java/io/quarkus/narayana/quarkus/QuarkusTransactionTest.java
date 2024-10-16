package io.quarkus.narayana.quarkus;

import static io.quarkus.narayana.jta.QuarkusTransaction.beginOptions;
import static io.quarkus.narayana.jta.QuarkusTransaction.runOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.TransactionSynchronizationRegistry;

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

    private static final AtomicInteger counter = new AtomicInteger();

    @Inject
    TransactionSynchronizationRegistry tsr;

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
        counter.set(0);

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

    @Test
    public void testConcurrentTransactionScopedBeanCreationWithSynchronization() {
        // test that propagating a transaction to other threads and use of Synchronizations do not interfere
        counter.set(0);

        // 1. A Transaction is activated in a parent thread.
        QuarkusTransaction.run(() -> {
            ExecutorService executor = Executors.newCachedThreadPool();

            try {
                Transaction txn = testBean.doWorkWithSynchronization(tsr, transactionManager);

                // 2. The parent thread starts 2 child threads, and propagates the transaction.
                // 3. The child threads access a @TransactionScoped bean concurrently,
                Future<Transaction> f1 = executor
                        .submit(threadContext
                                .contextualCallable(() -> testBean.doWorkWithSynchronization(tsr, transactionManager)));
                Future<Transaction> f2 = executor
                        .submit(threadContext
                                .contextualCallable(() -> testBean.doWorkWithSynchronization(tsr, transactionManager)));

                Transaction t1 = f1.get();
                Transaction t2 = f2.get();

                // the Synchronization callbacks for the parent thread and the two child threads should
                // all have run with the same transaction context
                Assertions.assertEquals(t1, txn);
                Assertions.assertEquals(t2, txn);
            } catch (Throwable e) {
                throw new AssertionError("Should not have thrown", e);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    @Test
    public void testConcurrentWithSynchronization() {
        // test that Synchronizations registered with concurrent transactions do not interfere
        Collection<Callable<Void>> callables = new ArrayList<>();
        IntStream.rangeClosed(1, 8)
                .forEach(i -> callables.add(() -> {
                    try {
                        // start a txn
                        // then register an interposed Synchronization
                        // then commit the txn
                        // and then verify the Synchronization ran with the same transaction
                        TestInterposedSync t = new TestInterposedSync(tsr, transactionManager);
                        transactionManager.begin();
                        Transaction txn = transactionManager.getTransaction();
                        tsr.registerInterposedSynchronization(t);
                        transactionManager.commit();
                        // check that the transaction seen by the Synchronization is same as the one we just started
                        Assertions.assertEquals(txn, t.getContext(), "Synchronization ran with the wrong context");
                    } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException
                            | HeuristicRollbackException | SecurityException | IllegalStateException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }));

        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            List<Future<Void>> futures = executor.invokeAll(callables);
            futures.forEach(f -> {
                try {
                    // verify that the task did not throw an exception
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new AssertionError("Should not have thrown", e);
                }
            });
        } catch (InterruptedException e) {
            throw new AssertionError("Should not have thrown", e);
        } finally {
            executor.shutdownNow();
        }
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

    static class TestInterposedSync implements Synchronization {
        private final TransactionManager tm;
        private Transaction context;
        int completionStatus = -1;

        public TestInterposedSync(TransactionSynchronizationRegistry tsr, TransactionManager tm) {
            this.tm = tm;
        }

        @Override
        public void beforeCompletion() {
            try {
                // remember the transaction context used to run the Synchronization
                context = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void afterCompletion(int status) {
            this.completionStatus = status;
        }

        public Transaction getContext() {
            // report the transaction context used to run the Synchronization
            return context;
        }
    }

    static class TransactionScopedTestBean {
        public void doWork() {

        }

        public Transaction doWorkWithSynchronization(TransactionSynchronizationRegistry tsr, TransactionManager tm) {
            TestInterposedSync t = new TestInterposedSync(tsr, tm);

            try {
                tsr.registerInterposedSynchronization(t);
                return tm.getTransaction();
            } catch (Exception e) {
                throw new AssertionError("Should not have thrown", e);
            }
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
