package io.quarkus.narayana.jta.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;

import com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.UserTransaction;

@Dependent
public class NarayanaJtaProducers {

    private static final javax.transaction.UserTransaction USER_TRANSACTION = UserTransaction.userTransaction();
    private static final com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple TRANSACTION_MANAGER = (TransactionManagerImple) TransactionManager
            .transactionManager();

    @Produces
    @ApplicationScoped
    public UserTransactionRegistry userTransactionRegistry() {
        return new UserTransactionRegistry();
    }

    @Produces
    @ApplicationScoped
    public javax.transaction.UserTransaction userTransaction() {
        return USER_TRANSACTION;
    }

    @Produces
    @ApplicationScoped
    public XAResourceRecoveryRegistry xaResourceRecoveryRegistry() {
        return new RecoveryManagerService();
    }

    @Produces
    @ApplicationScoped
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
        return new TransactionSynchronizationRegistryImple();
    }

    @Produces
    @Singleton
    public javax.transaction.TransactionManager transactionManager() {
        return TRANSACTION_MANAGER;
    }

    @Produces
    @ApplicationScoped
    public JBossXATerminator xaTerminator() {
        return new XATerminator();
    }
}
