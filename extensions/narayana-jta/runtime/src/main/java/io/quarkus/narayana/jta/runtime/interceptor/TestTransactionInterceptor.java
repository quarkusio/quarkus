package io.quarkus.narayana.jta.runtime.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.transaction.UserTransaction;

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
    UserTransaction userTransaction;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
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
        } finally {
            userTransaction.rollback();
        }
    }

}
