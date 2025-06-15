package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.narayana.jta.TransactionExceptionResult;
import io.quarkus.test.QuarkusUnitTest;

public class TransactionRunnerTest {

    @Inject
    TransactionManager transactionManager;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void commit() {
        var sync = new TestSync();
        QuarkusTransaction.requiringNew().run(() -> register(sync));
        assertEquals(Status.STATUS_COMMITTED, sync.completionStatus);

        assertEquals(Status.STATUS_COMMITTED, QuarkusTransaction.requiringNew().call(this::register).completionStatus);
    }

    @Test
    public void rollback() {
        var sync1 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            register(sync1);
            QuarkusTransaction.rollback();
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync1.completionStatus);

        var sync2 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().call(() -> {
            register(sync2);
            QuarkusTransaction.rollback();
            return null;
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync2.completionStatus);
    }

    @Test
    public void rollbackOnly() {
        var sync1 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            register(sync1);
            QuarkusTransaction.setRollbackOnly();
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync1.completionStatus);

        var sync2 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().call(() -> {
            register(sync2);
            QuarkusTransaction.setRollbackOnly();
            return null;
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync2.completionStatus);
    }

    @Test
    public void timeout() {
        var sync1 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().timeout(1).run(() -> {
            register(sync1);
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                fail("Interrupted unexpectedly");
            }
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync1.completionStatus);

        var sync2 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().timeout(1).call(() -> {
            register(sync2);
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                fail("Interrupted unexpectedly");
            }
            return null;
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync2.completionStatus);
    }

    @Test
    public void exception() {
        var sync1 = new TestSync();
        assertThrows(MyRuntimeException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            register(sync1);
            throw new MyRuntimeException();
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync1.completionStatus);

        var sync2 = new TestSync();
        assertThrows(MyRuntimeException.class, () -> QuarkusTransaction.requiringNew().call(() -> {
            register(sync2);
            throw new MyRuntimeException();
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync2.completionStatus);

        var sync3 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().call(() -> {
            register(sync3);
            throw new MyCheckedException();
        }));
        assertEquals(Status.STATUS_ROLLEDBACK, sync3.completionStatus);
    }

    @Test
    public void exceptionHandler() {
        var sync1 = new TestSync();
        assertThrows(MyRuntimeException.class, () -> QuarkusTransaction.requiringNew()
                .exceptionHandler((e) -> TransactionExceptionResult.COMMIT).run(() -> {
                    register(sync1);
                    throw new MyRuntimeException();
                }));
        assertEquals(Status.STATUS_COMMITTED, sync1.completionStatus);

        var sync2 = new TestSync();
        assertThrows(MyRuntimeException.class, () -> QuarkusTransaction.requiringNew()
                .exceptionHandler((e) -> TransactionExceptionResult.COMMIT).call(() -> {
                    register(sync2);
                    throw new MyRuntimeException();
                }));
        assertEquals(Status.STATUS_COMMITTED, sync2.completionStatus);

        var sync3 = new TestSync();
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew()
                .exceptionHandler((e) -> TransactionExceptionResult.COMMIT).call(() -> {
                    register(sync3);
                    throw new MyCheckedException();
                }));
        assertEquals(Status.STATUS_COMMITTED, sync3.completionStatus);
    }

    @Test
    @ActivateRequestContext
    public void suspendingExisting() {
        QuarkusTransaction.begin();
        assertTrue(QuarkusTransaction.isActive());
        QuarkusTransaction.suspendingExisting().run(() -> assertFalse(QuarkusTransaction.isActive()));
        assertTrue(QuarkusTransaction.isActive());
        QuarkusTransaction.rollback();

        assertFalse(QuarkusTransaction.isActive());
        QuarkusTransaction.suspendingExisting().run(() -> assertFalse(QuarkusTransaction.isActive()));
        assertFalse(QuarkusTransaction.isActive());
    }

    @Test
    @ActivateRequestContext
    public void disallowingExisting() {
        assertFalse(QuarkusTransaction.isActive());
        assertEquals(Status.STATUS_COMMITTED,
                QuarkusTransaction.disallowingExisting().call(this::register).completionStatus);
        assertFalse(QuarkusTransaction.isActive());

        QuarkusTransaction.begin();
        assertTrue(QuarkusTransaction.isActive());
        assertThrows(QuarkusTransactionException.class,
                () -> QuarkusTransaction.disallowingExisting().call(this::register));
        assertTrue(QuarkusTransaction.isActive());
        QuarkusTransaction.rollback();
    }

    @Test
    @ActivateRequestContext
    public void requiringNew() throws SystemException {
        assertFalse(QuarkusTransaction.isActive());
        assertEquals(Status.STATUS_COMMITTED, QuarkusTransaction.requiringNew().call(this::register).completionStatus);
        assertFalse(QuarkusTransaction.isActive());

        QuarkusTransaction.begin();
        assertTrue(QuarkusTransaction.isActive());
        var tx = transactionManager.getTransaction();
        assertEquals(Status.STATUS_COMMITTED, QuarkusTransaction.requiringNew().call(() -> {
            assertTrue(QuarkusTransaction.isActive());
            assertNotEquals(tx, transactionManager.getTransaction());
            return register();
        }).completionStatus);
        assertTrue(QuarkusTransaction.isActive());
        QuarkusTransaction.rollback();
    }

    @Test
    @ActivateRequestContext
    public void joiningExisting() throws SystemException {
        assertFalse(QuarkusTransaction.isActive());
        assertEquals(Status.STATUS_COMMITTED,
                QuarkusTransaction.joiningExisting().call(this::register).completionStatus);
        assertFalse(QuarkusTransaction.isActive());

        QuarkusTransaction.begin();
        assertTrue(QuarkusTransaction.isActive());
        var tx = transactionManager.getTransaction();
        QuarkusTransaction.joiningExisting().call(() -> {
            assertTrue(QuarkusTransaction.isActive());
            assertEquals(tx, transactionManager.getTransaction());
            return null;
        });
        assertTrue(QuarkusTransaction.isActive());
        QuarkusTransaction.rollback();
    }

    void register(TestSync t) {
        try {
            transactionManager.getTransaction().registerSynchronization(t);
        } catch (RollbackException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    TestSync register() {
        TestSync t = new TestSync();
        register(t);
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

    static class MyCheckedException extends Exception {
        MyCheckedException() {
        }
    }

    static class MyRuntimeException extends RuntimeException {
        MyRuntimeException() {
        }
    }

}
