package io.quarkus.narayana.interceptor;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.classloading.ClassLoaderLimiter;
import io.quarkus.test.QuarkusUnitTest;

public class TransactionalTest {

    static final Set<String> loadedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TransactionalTest.TransactionalBean.class, TestXAResource.class,
                            TxAssertionData.class, TestException.class))
            .addClassLoaderEventListener(ClassLoaderLimiter.builder()
                    .neverLoadedRuntimeClassName("javax.xml.stream.XMLInputFactory").build());

    @Inject
    private TransactionManager tm;

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private TransactionalTest.TransactionalBean testTransactionalBean;

    @Inject
    private TxAssertionData txAssertionData;

    @AfterEach
    public void tearDown() {
        try {
            userTransaction.rollback();
        } catch (Exception e) {
            // do nothing
        } finally {
            txAssertionData.reset();
        }
    }

    @Test
    public void transactionalRequiresToCommit() throws Exception {
        assertTransactionInactive();
        testTransactionalBean.executeTransactional();
        assertTransactionInactive();
        Assertions.assertEquals(1, txAssertionData.getCommit());
        Assertions.assertEquals(0, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowRuntimeException() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalThrowException(RuntimeException.class);
            Assertions.fail("Expecting RuntimeException to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(0, txAssertionData.getCommit());
        Assertions.assertEquals(1, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowApplicationException() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalThrowException(TestException.class);
            Assertions.fail("Expecting TestException to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(1, txAssertionData.getCommit());
        Assertions.assertEquals(0, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowError() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalThrowException(Error.class);
            Assertions.fail("Expecting Error to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(0, txAssertionData.getCommit());
        Assertions.assertEquals(1, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowApplicationExceptionWithRollbackOn() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalRollbackOnException(TestException.class);
            Assertions.fail("Expecting TestException to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(0, txAssertionData.getCommit());
        Assertions.assertEquals(1, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowRuntimeExceptionWithDontRollbackOn() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalDontRollbackOnRuntimeException(RuntimeException.class);
            Assertions.fail("Expecting RuntimeException to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(1, txAssertionData.getCommit());
        Assertions.assertEquals(0, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowErrorWithDontRollbackOn() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalDontRollbackOnError(Error.class);
            Assertions.fail("Expecting Error to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(1, txAssertionData.getCommit());
        Assertions.assertEquals(0, txAssertionData.getRollback());
    }

    @Test
    public void transactionalThrowApplicationExceptionDontRollbackOnPriority() {
        assertTransactionInactive();
        try {
            testTransactionalBean.executeTransactionalRollbackOnPriority(TestException.class);
            Assertions.fail("Expecting TestException to be thrown and the execution does not reach this point");
        } catch (Throwable expected) {
        }
        assertTransactionInactive();
        Assertions.assertEquals(1, txAssertionData.getCommit());
        Assertions.assertEquals(0, txAssertionData.getRollback());
    }

    private void assertTransactionInactive() {
        try {
            if (tm.getTransaction() != null) {
                Assertions.assertNotEquals(Status.STATUS_ACTIVE, tm.getTransaction().getStatus());
            }
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @ApplicationScoped
    static class TransactionalBean {
        @Inject
        private TransactionManager transactionManager;

        @Inject
        private TxAssertionData txAssertionData;

        private void enlist() throws SystemException, RollbackException {
            transactionManager.getTransaction()
                    .enlistResource(new TestXAResource(txAssertionData));
        }

        @Transactional
        public void executeTransactional() throws Exception {
            enlist();
        }

        @Transactional
        public void executeTransactionalThrowException(Class<? extends Throwable> throwable) throws Throwable {
            enlist();
            throw throwable.newInstance();
        }

        @Transactional(rollbackOn = Exception.class)
        public void executeTransactionalRollbackOnException(Class<? extends Throwable> throwable) throws Throwable {
            enlist();
            throw throwable.newInstance();
        }

        @Transactional(dontRollbackOn = RuntimeException.class)
        public void executeTransactionalDontRollbackOnRuntimeException(Class<? extends Throwable> throwable) throws Throwable {
            enlist();
            throw throwable.newInstance();
        }

        @Transactional(dontRollbackOn = Error.class)
        public void executeTransactionalDontRollbackOnError(Class<? extends Throwable> throwable) throws Throwable {
            enlist();
            throw throwable.newInstance();
        }

        @Transactional(dontRollbackOn = Exception.class, rollbackOn = Exception.class)
        public void executeTransactionalRollbackOnPriority(Class<? extends Throwable> throwable) throws Throwable {
            enlist();
            throw throwable.newInstance();
        }
    }
}
