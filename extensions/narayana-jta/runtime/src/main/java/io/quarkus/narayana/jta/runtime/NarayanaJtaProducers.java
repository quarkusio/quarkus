package io.quarkus.narayana.jta.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;

import com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

import io.quarkus.arc.Unremovable;
import io.quarkus.narayana.jta.runtime.internal.tsr.TransactionSynchronizationRegistryWrapper;

@Dependent
public class NarayanaJtaProducers {

    @Produces
    @ApplicationScoped
    public UserTransactionRegistry userTransactionRegistry() {
        return new UserTransactionRegistry();
    }

    @Produces
    @ApplicationScoped
    public UserTransaction userTransaction() {
        return new NotifyingUserTransaction(com.arjuna.ats.jta.UserTransaction.userTransaction());
    }

    @Produces
    @Unremovable
    @Singleton
    public jakarta.transaction.TransactionManager transactionManager() {
        return new NotifyingTransactionManager();
    }

    @Produces
    @Singleton
    public XAResourceRecoveryRegistry xaResourceRecoveryRegistry() {
        return QuarkusRecoveryService.getInstance();
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
        return new TransactionSynchronizationRegistryWrapper(new TransactionSynchronizationRegistryImple());
    }

    @Produces
    @ApplicationScoped
    public JBossXATerminator xaTerminator() {
        return new XATerminator();
    }
}
