package io.quarkus.hibernate.orm.runtime.customized;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;

public final class QuarkusJtaPlatform extends AbstractJtaPlatform {

    public static final QuarkusJtaPlatform INSTANCE = new QuarkusJtaPlatform();

    private QuarkusJtaPlatform() {
        //nothing
    }

    @Override
    protected TransactionManager locateTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Override
    protected UserTransaction locateUserTransaction() {
        return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

}
