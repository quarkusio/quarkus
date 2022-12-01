package io.quarkus.narayana.jta.runtime.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional;
import javax.transaction.TransactionalException;

import com.arjuna.ats.jta.logging.jtaLogger;

/**
 * @author paul.robinson@redhat.com 25/05/2013
 */

@Interceptor
@Transactional(Transactional.TxType.MANDATORY)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class TransactionalInterceptorMandatory extends TransactionalInterceptorBase {
    public TransactionalInterceptorMandatory() {
        super(false);
    }

    @Override
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return super.intercept(ic);
    }

    @Override
    protected Object doIntercept(TransactionManager tm, Transaction tx, InvocationContext ic) throws Exception {
        if (tx == null) {
            throw new TransactionalException(jtaLogger.i18NLogger.get_tx_required(), new TransactionRequiredException());
        }
        return invokeInCallerTx(ic, tx);
    }
}
