package io.quarkus.narayana.interceptor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;

/**
 * A bean enlisting a {@link TestXAResource} in the transaction it starts, so that tests can tell a commit from a
 * rollback, and then failing in the various ways by throwing different exception types
 * that the {@code quarkus.transactional.checked-exceptions} property is meant to discriminate between.
 */
@ApplicationScoped
public class TransactionalCheckedExceptionBean {

    @Inject
    TransactionManager transactionManager;

    @Inject
    TxAssertionData txAssertionData;

    private void doEnlist() throws SystemException, RollbackException {
        transactionManager.getTransaction().enlistResource(new TestXAResource(txAssertionData));
    }

    @Transactional
    public void throwCheckedException() throws Exception {
        doEnlist();
        throw new TestException();
    }

    /**
     * The exception class carries {@code @Rollback}, which overrides whatever
     * {@code quarkus.transactional.checked-exceptions} is set to.
     */
    @Transactional
    public void throwAnnotatedCheckedException() throws Exception {
        doEnlist();
        throw new AnnotatedTestException();
    }

    /**
     * A method annotated with {@code dontRollbackOn}, which overrides whatever
     * {@code quarkus.transactional.checked-exceptions} is set to.
     */
    @Transactional(dontRollbackOn = TestException.class)
    public void throwCheckedExceptionDontRollbackOn() throws Exception {
        doEnlist();
        throw new TestException();
    }

    /**
     * Unchecked exceptions are unaffected by {@code quarkus.transactional.checked-exceptions}.
     */
    @Transactional
    public void throwRuntimeException() throws Exception {
        doEnlist();
        throw new IllegalStateException("Should rollback here");
    }

    /**
     * Baseline test
     */
    @Transactional
    public void succeed() throws Exception {
        doEnlist();
    }
}
