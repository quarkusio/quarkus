package io.quarkus.hibernate.orm.runtime.customized;

import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.internal.TransactionManagerAccess;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

public final class QuarkusJtaPlatform implements JtaPlatform, TransactionManagerAccess {

    public static final QuarkusJtaPlatform INSTANCE = new QuarkusJtaPlatform();

    private volatile TransactionManager transactionManager;
    private volatile UserTransaction userTransaction;

    private QuarkusJtaPlatform() {
        //nothing
    }

    @Override
    public TransactionManager retrieveTransactionManager() {
        TransactionManager transactionManager = this.transactionManager;
        if (transactionManager == null) {
            transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
            this.transactionManager = transactionManager;
        }
        return transactionManager;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return retrieveTransactionManager();
    }

    @Override
    public UserTransaction retrieveUserTransaction() {
        UserTransaction userTransaction = this.userTransaction;
        if (userTransaction == null) {
            userTransaction = com.arjuna.ats.jta.UserTransaction.userTransaction();
            this.userTransaction = userTransaction;
        }
        return userTransaction;
    }

    @Override
    public Object getTransactionIdentifier(final Transaction transaction) {
        return transaction;
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) {
        try {
            getTransactionManager().getTransaction().registerSynchronization(synchronization);
        } catch (Exception e) {
            throw new JtaPlatformException("Could not access JTA Transaction to register synchronization", e);
        }
    }

    @Override
    public boolean canRegisterSynchronization() {
        return JtaStatusHelper.isActive(getTransactionManager());
    }

    @Override
    public int getCurrentStatus() throws SystemException {
        return this.retrieveTransactionManager().getStatus();
    }

}
