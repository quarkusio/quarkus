package io.quarkus.hibernate.orm.runtime.customized;

import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;
import org.hibernate.engine.transaction.jta.platform.internal.TransactionManagerAccess;
import org.hibernate.engine.transaction.jta.platform.internal.TransactionManagerBasedSynchronizationStrategy;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

public final class QuarkusJtaPlatform implements JtaPlatform, TransactionManagerAccess {

    public static final QuarkusJtaPlatform INSTANCE = new QuarkusJtaPlatform();

    private final JtaSynchronizationStrategy tmSynchronizationStrategy = new TransactionManagerBasedSynchronizationStrategy(
            this);
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
        if (this.userTransaction == null) {
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
        this.tmSynchronizationStrategy.registerSynchronization(synchronization);
    }

    @Override
    public boolean canRegisterSynchronization() {
        return this.tmSynchronizationStrategy.canRegisterSynchronization();
    }

    @Override
    public int getCurrentStatus() throws SystemException {
        return this.retrieveTransactionManager().getStatus();
    }

}
