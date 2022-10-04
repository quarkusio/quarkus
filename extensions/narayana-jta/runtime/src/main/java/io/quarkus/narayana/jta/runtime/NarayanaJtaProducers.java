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
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.UserTransaction;

import io.quarkus.arc.Unremovable;

@Dependent
public class NarayanaJtaProducers {

    @Produces
    @ApplicationScoped
    public UserTransactionRegistry userTransactionRegistry() {
        return new UserTransactionRegistry();
    }

    @Produces
    @ApplicationScoped
    public javax.transaction.UserTransaction userTransaction() {
        return UserTransaction.userTransaction();
    }

    @Produces
    @Singleton
    public XAResourceRecoveryRegistry xaResourceRecoveryRegistry(TransactionManagerConfiguration config) {
        RecoveryManagerService recoveryManagerService = new RecoveryManagerService();
        if (config.enableRecovery) {
            recoveryManagerService.create();
            recoveryManagerService.start();
        }
        return recoveryManagerService;
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
        return new TransactionSynchronizationRegistryImple();
    }

    @Produces
    @ApplicationScoped
    public JBossXATerminator xaTerminator() {
        return new XATerminator();
    }
}
