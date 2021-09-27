package io.quarkus.narayana.jta.runtime.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;

/**
 * @author paul.robinson@redhat.com 25/05/2013
 */

@Interceptor
@Transactional(Transactional.TxType.REQUIRES_NEW)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class TransactionalInterceptorRequiresNew extends TransactionalInterceptorBase {
    public TransactionalInterceptorRequiresNew() {
        super(false);
    }

    @Override
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new BlockingOperationNotAllowedException("Cannot start a JTA transaction from the IO thread.");
        }
        return super.intercept(ic);
    }

    @Override
    protected Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception {
        if (tx != null) {
            tm.suspend();
            return invokeInOurTx(ic, tm, () -> tm.resume(tx));
        } else {
            return invokeInOurTx(ic, tm);
        }
    }
}
