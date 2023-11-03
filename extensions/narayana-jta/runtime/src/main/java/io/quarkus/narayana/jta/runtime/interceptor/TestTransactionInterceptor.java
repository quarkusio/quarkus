package io.quarkus.narayana.jta.runtime.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import io.quarkus.narayana.jta.runtime.test.TestTransactionCallback;

public class TestTransactionInterceptor {

    static final List<TestTransactionCallback> CALLBACKS;

    static {
        List<TestTransactionCallback> callbacks = new ArrayList<>();
        for (TestTransactionCallback i : ServiceLoader.load(TestTransactionCallback.class)) {
            callbacks.add(i);
        }
        CALLBACKS = callbacks;
    }

    @Inject
    public UserTransaction userTransaction;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // do nothing in case there is already a transaction (e.g. self-intercepted non-private non-test method in test class)
        // w/o this check userTransaction.begin() would fail because there is already a tx associated with the current thread
        if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
            return context.proceed();
        }

        // an exception from proceed() has to be captured to avoid shadowing it in finally() with an exception from rollback()
        Throwable caught = null;
        try {
            userTransaction.begin();
            for (TestTransactionCallback i : CALLBACKS) {
                i.postBegin();
            }
            Object result = context.proceed();
            for (TestTransactionCallback i : CALLBACKS) {
                i.preRollback();
            }
            return result;
        } catch (Exception | Error e) { // note: "Error" shall mainly address AssertionError
            caught = e;
            throw e;
        } finally {
            if (caught == null) {
                userTransaction.rollback();
            } else {
                try {
                    userTransaction.rollback();
                } catch (Exception e) {
                    caught.addSuppressed(e);
                }
            }
        }
    }
}
