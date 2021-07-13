package io.quarkus.narayana.jta.runtime.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.tm.usertx.client.ServerVMClientUserTransaction;
import org.reactivestreams.Publisher;

import com.arjuna.ats.jta.logging.jtaLogger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.narayana.jta.runtime.CDIDelegatingTransactionManager;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;

public abstract class TransactionalInterceptorBase implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(TransactionalInterceptorBase.class);
    private final Map<Method, Integer> methodTransactionTimeoutDefinedByPropertyCache = new ConcurrentHashMap<>();

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
     * @param ic invocation context of the interceptor
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
        if (configuration == null) {
            Class<?> clazz;
            Object target = ic.getTarget();
            if (target != null) {
                clazz = target.getClass();
            } else {
                // Very likely an intercepted static method
                clazz = ic.getMethod().getDeclaringClass();
            }
            return clazz.getAnnotation(TransactionConfiguration.class);
        }
        return configuration;
    }

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm) throws Exception {
        return invokeInOurTx(ic, tm, () -> {
        });
    }

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm, RunnableWithException afterEndTransaction)
            throws Exception {

        int timeoutConfiguredForMethod = getTransactionTimeoutFromAnnotation(ic);

        int currentTmTimeout = ((CDIDelegatingTransactionManager) transactionManager).getTransactionTimeout();

        if (timeoutConfiguredForMethod > 0) {
            tm.setTransactionTimeout(timeoutConfiguredForMethod);
        }

        Transaction tx;
        try {
            tm.begin();
            tx = tm.getTransaction();
        } finally {
            if (timeoutConfiguredForMethod > 0) {
                tm.setTransactionTimeout(currentTmTimeout);
            }
        }

        boolean throwing = false;
        Object ret = null;

        try {
            ret = ic.proceed();
        } catch (Throwable t) {
            throwing = true;
            handleException(ic, t, tx);
        } finally {
            // handle asynchronously if not throwing
            if (!throwing && ret != null) {
                ReactiveTypeConverter<Object> converter = null;
                if (ret instanceof CompletionStage == false
                        && (ret instanceof Publisher == false || ic.getMethod().getReturnType() != Publisher.class)) {
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

    private int getTransactionTimeoutFromAnnotation(InvocationContext ic) {
        TransactionConfiguration configAnnotation = getTransactionConfiguration(ic);

        if (configAnnotation == null) {
            return -1;
        }

        int transactionTimeout = -1;

        if (!configAnnotation.timeoutFromConfigProperty().equals(TransactionConfiguration.UNSET_TIMEOUT_CONFIG_PROPERTY)) {
            Integer timeoutForMethod = methodTransactionTimeoutDefinedByPropertyCache.get(ic.getMethod());
            if (timeoutForMethod != null) {
                transactionTimeout = timeoutForMethod;
            } else {
                transactionTimeout = methodTransactionTimeoutDefinedByPropertyCache.computeIfAbsent(ic.getMethod(),
                        new Function<Method, Integer>() {
                            @Override
                            public Integer apply(Method m) {
                                return TransactionalInterceptorBase.this.getTransactionTimeoutPropertyValue(configAnnotation);
                            }
                        });
            }
        }

        if (transactionTimeout == -1 && (configAnnotation.timeout() != TransactionConfiguration.UNSET_TIMEOUT)) {
            transactionTimeout = configAnnotation.timeout();
        }

        return transactionTimeout;
    }

    private Integer getTransactionTimeoutPropertyValue(TransactionConfiguration configAnnotation) {
        Optional<Integer> configTimeout = ConfigProvider.getConfig()
                .getOptionalValue(configAnnotation.timeoutFromConfigProperty(), Integer.class);
        if (configTimeout.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debugf("Configuration property '%s' was not provided, so it will not affect the transaction's timeout.",
                        configAnnotation.timeoutFromConfigProperty());
            }
            return -1;
        }

        return configTimeout.get();
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
            ret = Multi.createFrom().publisher((Publisher<?>) ret)
                    .onFailure().invoke(t -> {
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
                    }).onTermination().invoke(() -> {
                        try {
                            doInTransaction(tm, tx, () -> endTransaction(tm, tx, () -> {
                            }));
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
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
        } catch (Throwable t) {
            handleException(ic, t, tx);
        }
        throw new RuntimeException("UNREACHABLE");
    }

    protected Object invokeInNoTx(InvocationContext ic) throws Exception {
        checkConfiguration(ic);
        return ic.proceed();
    }

    private void checkConfiguration(InvocationContext ic) {
        TransactionConfiguration configAnnotation = getTransactionConfiguration(ic);
        if (configAnnotation != null && ((configAnnotation.timeout() != TransactionConfiguration.UNSET_TIMEOUT)
                || !TransactionConfiguration.UNSET_TIMEOUT_CONFIG_PROPERTY
                        .equals(configAnnotation.timeoutFromConfigProperty()))) {
            throw new RuntimeException("Changing timeout via @TransactionConfiguration can only be done " +
                    "at the entry level of a transaction");
        }
    }

    protected void handleExceptionNoThrow(InvocationContext ic, Throwable t, Transaction tx)
            throws IllegalStateException, SystemException {

        Transactional transactional = getTransactional(ic);

        for (Class<?> dontRollbackOnClass : transactional.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(t.getClass())) {
                return;
            }
        }

        for (Class<?> rollbackOnClass : transactional.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(t.getClass())) {
                tx.setRollbackOnly();
                return;
            }
        }

        // RuntimeException and Error are un-checked exceptions and rollback is expected
        if (t instanceof RuntimeException || t instanceof Error) {
            tx.setRollbackOnly();
            return;
        }
    }

    protected void handleException(InvocationContext ic, Throwable t, Transaction tx) throws Exception {

        handleExceptionNoThrow(ic, t, tx);
        sneakyThrow(t);
    }

    protected void endTransaction(TransactionManager tm, Transaction tx, RunnableWithException afterEndTransaction)
            throws Exception {
        try {
            if (tx != tm.getTransaction()) {
                throw new RuntimeException(jtaLogger.i18NLogger.get_wrong_tx_on_thread());
            }

            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                tm.rollback();
            } else {
                tm.commit();
            }
        } finally {
            afterEndTransaction.run();
        }
    }

    protected boolean setUserTransactionAvailable(boolean available) {
        boolean previousUserTransactionAvailability = ServerVMClientUserTransaction.isAvailable();

        ServerVMClientUserTransaction.setAvailability(available);

        return previousUserTransactionAvailability;
    }

    protected void resetUserTransactionAvailability(boolean previousUserTransactionAvailability) {
        ServerVMClientUserTransaction.setAvailability(previousUserTransactionAvailability);
    }

    /**
     * An utility method to throw any exception as a {@link RuntimeException}.
     * We may throw a checked exception (subtype of {@code Throwable} or {@code Exception}) as un-checked exception.
     * This considers the Java 8 inference rule that states that a {@code throws E} is inferred as {@code RuntimeException}.
     * <p>
     * This method can be used in {@code throw} statement such as: {@code throw sneakyThrow(exception);}.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
