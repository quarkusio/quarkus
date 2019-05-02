/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.narayana.jta.runtime.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

import org.jboss.tm.usertx.client.ServerVMClientUserTransaction;

import com.arjuna.ats.jta.logging.jtaLogger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.runtime.InterceptorBindings;

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

    protected Object invokeInOurTx(InvocationContext ic, TransactionManager tm) throws Exception {

        tm.begin();
        Transaction tx = tm.getTransaction();
        boolean throwing = false;

        try {
            return ic.proceed();
        } catch (Exception e) {
            handleException(ic, e, tx);
            throwing = true;
        } finally {
            // do not listen to async notifications if we're throwing
            if (throwing || !handleIfAsyncStarted(tm, tx, ic)) {
                endTransaction(tm, tx);
            }
        }
        throw new RuntimeException("UNREACHABLE");
    }

    protected boolean handleIfAsyncStarted(TransactionManager tm, Transaction tx, InvocationContext ic) {
        ArcContainer arcContainer = Arc.container();
        if (arcContainer.isCurrentRequestAsync()) {
            arcContainer.getAsyncRequestNotifier().handle((v, t) -> {
                try {
                    if (t != null)
                        handleExceptionNoThrow(ic, t, tx);
                    endTransaction(tm, tx);
                } catch (Exception e) {
                    jtaLogger.logger.error("Failed to end async transaction", e);
                }
                return null;
            });
            return true;
        }
        return false;
    }

    protected Object invokeInCallerTx(InvocationContext ic, Transaction tx) throws Exception {

        try {
            return ic.proceed();
        } catch (Exception e) {
            handleException(ic, e, tx);
        }
        throw new RuntimeException("UNREACHABLE");
    }

    protected Object invokeInNoTx(InvocationContext ic) throws Exception {

        return ic.proceed();
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

    protected void endTransaction(TransactionManager tm, Transaction tx) throws Exception {

        if (tx != tm.getTransaction()) {
            throw new RuntimeException(jtaLogger.i18NLogger.get_wrong_tx_on_thread());
        }

        if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            tm.rollback();
        } else {
            tm.commit();
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
}
