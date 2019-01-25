package org.jboss.shamrock.test.dataaccess;

import javax.annotation.Priority;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

@TestTransaction
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class TestTransactionInterceptor {

    @Inject
    UserTransaction userTransaction;

    @Inject
    @Any
    Instance<EntityManager> entityManagers;

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        userTransaction.begin();
        try {
            Object result = ctx.proceed();
            int status = userTransaction.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                for (EntityManager i : entityManagers) {
                    i.flush();
                }
            }
            return result;
        } finally {
            try {
                userTransaction.rollback();
            } catch (Exception e) {
                //we want the original exception
                e.printStackTrace();
            }
        }

    }

}
