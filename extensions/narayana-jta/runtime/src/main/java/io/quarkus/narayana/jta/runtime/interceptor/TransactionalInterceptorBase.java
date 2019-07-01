package io.quarkus.narayana.jta.runtime.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.tm.usertx.client.ServerVMClientUserTransaction;
import org.reactivestreams.Publisher;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.jta.logging.jtaLogger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;

/**
 * @author paul.robinson@redhat.com 02/05/2013
 */

public abstract class TransactionalInterceptorBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    TransactionManager transactionManager;

    private final boolean userTransactionAvailable;

    protected TransactionalInterceptorBase(boolean userTransactionAvailable) {
        this.userTransactionAvailable = userTransactionAvailable;
    }

    public Object intercept(InvocationContext ic) throws Exception {
        final TransactionManager tm = transactionManager;
        final Transaction tx = tm.getTransaction();

        boolean previousUserTransactionAvailability = setUserTransactionAvailable(userTransactionAvailable);
        try {
            return doIntercept(tm, tx, ic);
        } finally {
            resetUserTransactionAvailability(previousUserTransactionAvailability);
        }
    }

    protected abstract Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception;

    /**
     * <p>
     * Looking for the {@link Transactional} annotation first on the method,
     * second on the class.
     * <p>
     * Method handles CDI types to cover cases where extensions are used. In
     * case of EE container uses reflection.
     *
     * @param ic
     *        invocation context of the interceptor
     * @return instance of {@link Transactional} annotation or null
     */
    private Transactional getTransactional(InvocationContext ic) {
        Set<Annotation> bindings = InterceptorBindings.getInterceptorBindings(ic);
        for (Annotation i : bindings) {
            if (i.annotationType() == Transactional.class) {
                return (Transactional) i;
            }
        }
        throw new RuntimeException(jtaLogger.i18NLogger.get_expected_transactional_annotation());
    }

    private TransactionConfiguration getTransactionConfiguration(InvocationContext ic) {
        TransactionConfiguration configuration = ic.getMethod().getAnnotation(TransactionConfiguration.class);
        if (ic == null) {
            return ic.getTarget().getClass().getAnnotation(TransactionConfiguration.class);
        }
        return configuration;
    }

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm) throws Exception {
        return invokeInOurTx(ic, tm, () -> {
        });
    }

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm, RunnableWithException afterEndTransaction)
            throws Exception {

        TransactionConfiguration configAnnotation = getTransactionConfiguration(ic);
        int currentTmTimeout = ((TransactionManagerImple) com.arjuna.ats.jta.TransactionManager.transactionManager())
                .getTimeout();
        if (configAnnotation != null && configAnnotation.timeout() != TransactionConfiguration.UNSET_TIMEOUT) {
            tm.setTransactionTimeout(configAnnotation.timeout());
        }
        Transaction tx;
        try {
            tm.begin();
            tx = tm.getTransaction();
        } finally {
            if (configAnnotation != null && configAnnotation.timeout() != TransactionConfiguration.UNSET_TIMEOUT) {
                //restore the default behaviour
                tm.setTransactionTimeout(currentTmTimeout);
            }
        }

        boolean throwing = false;
        Object ret = null;

        try {
            ret = ic.proceed();
        } catch (Exception e) {
            throwing = true;
            handleException(ic, e, tx);
        } finally {
            // handle asynchronously if not throwing
            if (!throwing && ret != null) {
                ReactiveTypeConverter<Object> converter = null;
                if (ret instanceof CompletionStage == false
                        && ret instanceof Publisher == false) {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    Optional<ReactiveTypeConverter<Object>> lookup = Registry.lookup((Class) ret.getClass());
                    if (lookup.isPresent()) {
                        converter = lookup.get();
                        if (converter.emitAtMostOneItem()) {
                            ret = converter.toCompletionStage(ret);
                        } else {
                            ret = converter.toRSPublisher(ret);
                        }
                    }
                }
                if (ret instanceof CompletionStage) {
                    ret = handleAsync(tm, tx, ic, ret, afterEndTransaction);
                    // convert back
                    if (converter != null)
                        ret = converter.fromCompletionStage((CompletionStage<?>) ret);
                } else if (ret instanceof Publisher) {
                    ret = handleAsync(tm, tx, ic, ret, afterEndTransaction);
                    // convert back
                    if (converter != null)
                        ret = converter.fromPublisher((Publisher<?>) ret);
                } else {
                    // not async: handle synchronously
                    endTransaction(tm, tx, afterEndTransaction);
                }
            } else {
                // throwing or null: handle synchronously
                endTransaction(tm, tx, afterEndTransaction);
            }
        }
        return ret;
    }

    protected Object handleAsync(TransactionManager tm, Transaction tx, InvocationContext ic, Object ret,
            RunnableWithException afterEndTransaction) throws Exception {
        // Suspend the transaction to remove it from the main request thread
        tm.suspend();
        afterEndTransaction.run();
        if (ret instanceof CompletionStage) {
            return ((CompletionStage<?>) ret).handle((v, t) -> {
                try {
                    doInTransaction(tm, tx, () -> {
                        if (t != null)
                            handleExceptionNoThrow(ic, t, tx);
                        endTransaction(tm, tx, () -> {
                        });
                    });
                } catch (RuntimeException e) {
                    if (t != null)
                        e.addSuppressed(t);
                    throw e;
                } catch (Exception e) {
                    CompletionException x = new CompletionException(e);
                    if (t != null)
                        x.addSuppressed(t);
                    throw x;
                }
                // pass-through the previous results
                if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                if (t != null)
                    throw new CompletionException(t);
                return v;
            });
        } else if (ret instanceof Publisher) {
            ret = ReactiveStreams.fromPublisher(((Publisher<?>) ret))
                    .onError(t -> {
                        try {
                            doInTransaction(tm, tx, () -> handleExceptionNoThrow(ic, t, tx));
                        } catch (RuntimeException e) {
                            e.addSuppressed(t);
                            throw e;
                        } catch (Exception e) {
                            RuntimeException x = new RuntimeException(e);
                            x.addSuppressed(t);
                            throw x;
                        }
                        // pass-through the previous result
                        if (t instanceof RuntimeException)
                            throw (RuntimeException) t;
                        throw new RuntimeException(t);
                    }).onTerminate(() -> {
                        try {
                            doInTransaction(tm, tx, () -> endTransaction(tm, tx, () -> {
                            }));
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            RuntimeException x = new RuntimeException(e);
                            throw x;
                        }
                    })
                    .buildRs();
        }
        return ret;
    }

    private void doInTransaction(TransactionManager tm, Transaction tx, RunnableWithException f) throws Exception {
        // Verify if this thread's transaction is the right one
        Transaction currentTransaction = tm.getTransaction();
        // If not, install the right transaction
        if (currentTransaction != tx) {
            if (currentTransaction != null)
                tm.suspend();
            tm.resume(tx);
        }
        f.run();
        if (currentTransaction != tx) {
            tm.suspend();
            if (currentTransaction != null)
                tm.resume(currentTransaction);
        }
    }

    protected Object invokeInCallerTx(InvocationContext ic, Transaction tx) throws Exception {
        try {
            checkConfiguration(ic);
            return ic.proceed();
        } catch (Exception e) {
            handleException(ic, e, tx);
        }
        throw new RuntimeException("UNREACHABLE");
    }

    protected Object invokeInNoTx(InvocationContext ic) throws Exception {
        checkConfiguration(ic);
        return ic.proceed();
    }

    private void checkConfiguration(InvocationContext ic) {
        TransactionConfiguration configAnnotation = getTransactionConfiguration(ic);
        if (configAnnotation != null && configAnnotation.timeout() != TransactionConfiguration.UNSET_TIMEOUT) {
            throw new RuntimeException("Changing timeout via @TransactionConfiguration can only be done " +
                    "at the entry level of a transaction");
        }
    }

    protected void handleExceptionNoThrow(InvocationContext ic, Throwable e, Transaction tx)
            throws IllegalStateException, SystemException {

        Transactional transactional = getTransactional(ic);

        for (Class<?> dontRollbackOnClass : transactional.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(e.getClass())) {
                return;
            }
        }

        for (Class<?> rollbackOnClass : transactional.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(e.getClass())) {
                tx.setRollbackOnly();
                return;
            }
        }

        if (e instanceof RuntimeException) {
            tx.setRollbackOnly();
            return;
        }
    }

    protected void handleException(InvocationContext ic, Exception e, Transaction tx) throws Exception {

        handleExceptionNoThrow(ic, e, tx);
        throw e;
    }

    protected void endTransaction(TransactionManager tm, Transaction tx, RunnableWithException afterEndTransaction)
            throws Exception {
        if (tx != tm.getTransaction()) {
            throw new RuntimeException(jtaLogger.i18NLogger.get_wrong_tx_on_thread());
        }

        if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            tm.rollback();
        } else {
            tm.commit();
        }

        afterEndTransaction.run();
    }

    protected boolean setUserTransactionAvailable(boolean available) {

        boolean previousUserTransactionAvailability = ServerVMClientUserTransaction.isAvailable();

        ServerVMClientUserTransaction.setAvailability(available);

        return previousUserTransactionAvailability;
    }

    protected void resetUserTransactionAvailability(boolean previousUserTransactionAvailability) {
        ServerVMClientUserTransaction.setAvailability(previousUserTransactionAvailability);
    }
}
