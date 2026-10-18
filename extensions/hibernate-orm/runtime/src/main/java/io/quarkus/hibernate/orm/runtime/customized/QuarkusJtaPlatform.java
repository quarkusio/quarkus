package io.quarkus.hibernate.orm.runtime.customized;

import static jakarta.transaction.Status.STATUS_ACTIVE;

import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.TransactionManagerAccess;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;

import io.quarkus.arc.Arc;

public final class QuarkusJtaPlatform implements JtaPlatform, TransactionManagerAccess {

    public static final QuarkusJtaPlatform INSTANCE = new QuarkusJtaPlatform();

    private volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private volatile TransactionManager transactionManager;
    private volatile UserTransaction userTransaction;

    private QuarkusJtaPlatform() {
        //nothing
    }

    public TransactionSynchronizationRegistry retrieveTransactionSynchronizationRegistry() {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry = this.transactionSynchronizationRegistry;
        if (transactionSynchronizationRegistry == null) {
            transactionSynchronizationRegistry = Arc.container().instance(TransactionSynchronizationRegistry.class).get();

            this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        }
        return transactionSynchronizationRegistry;
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
        // no need to check STATUS_MARKED_ROLLBACK since synchronizations can't be registered in that state
        return retrieveTransactionSynchronizationRegistry().getTransactionStatus() == STATUS_ACTIVE;
    }

    @Override
    public int getCurrentStatus() throws SystemException {
        return this.retrieveTransactionManager().getStatus();
    }

}
