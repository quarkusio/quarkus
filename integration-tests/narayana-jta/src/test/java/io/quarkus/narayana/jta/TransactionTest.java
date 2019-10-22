package io.quarkus.narayana.jta;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxStats;
import com.arjuna.ats.jta.common.jtaPropertyManager;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TransactionTest {
    @Inject
    private UserTransaction tx;

    @Inject
    private TransactionManager tm;

    /*
     * verify that @Transactional works
     */
    @Transactional
    public void cmt() throws SystemException {
        Transaction txn = tm.getTransaction();

        Assertions.assertEquals(STATUS_ACTIVE, txn.getStatus());
    }

    /*
     * verify that applications can start and stop transactions and enlist with them
     */
    @Test
    void bmt() throws SystemException, HeuristicRollbackException, HeuristicMixedException, RollbackException,
            NotSupportedException {
        tm.begin();

        tm.getTransaction().enlistResource(new TestXAResource());
        tm.getTransaction().enlistResource(new TestXAResource());

        Assertions.assertEquals(STATUS_ACTIVE, tx.getStatus());

        tm.commit();

        // both XA resources should have been told to commit
        Assertions.assertEquals(2, TestXAResource.getCommitCount());

        // and the transactions should have ended
        Assertions.assertTrue(tx.getStatus() == STATUS_NO_TRANSACTION || tx.getStatus() == STATUS_COMMITTED);
    }

    /*
     * verify that an application that uses JTA can be configured
     */
    @Test
    void testDefaultProperties() {
        // verify that the quarkus configuration took effect
        Assertions.assertEquals("quarkus", arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier());
        Assertions.assertEquals(300, arjPropertyManager.getCoordinatorEnvironmentBean().getDefaultTimeout());
        Assertions.assertEquals("target/tx-object-store", // this value is set via application.properties
                arjPropertyManager.getObjectStoreEnvironmentBean().getObjectStoreDir());

        // verify that Narayana environment configuration properties can be set via system properties
        Assertions.assertLinesMatch(Arrays.asList(
                "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateJTAXAResourceOrphanFilter",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter"),
                jtaPropertyManager.getJTAEnvironmentBean().getXaResourceOrphanFilterClassNames());
        Assertions.assertLinesMatch(Arrays.asList(
                "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule",
                "com.arjuna.ats.internal.txoj.recovery.TORecoveryModule",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule"),
                recoveryPropertyManager.getRecoveryEnvironmentBean().getRecoveryModuleClassNames());
        Assertions.assertLinesMatch(Collections.singletonList(
                "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner"),
                recoveryPropertyManager.getRecoveryEnvironmentBean().getExpiryScannerClassNames());
        Assertions.assertEquals(4712, recoveryPropertyManager.getRecoveryEnvironmentBean().getRecoveryPort());
        Assertions.assertTrue(recoveryPropertyManager.getRecoveryEnvironmentBean().isRecoveryListener());

        Assertions.assertLinesMatch(Collections.singletonList(arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()),
                jtaPropertyManager.getJTAEnvironmentBean().getXaRecoveryNodes());
        Assertions.assertFalse(arjPropertyManager.getCoordinatorEnvironmentBean().isTransactionStatusManagerEnable());
    }

    /*
     * Verify that system properties can override the default Narayana config if the quarkus property
     * quarkus.transaction-manager.checkForNarayanaSystemProperties is set to true
     */
    @Test
    void testSystemPropertyOverride()
            throws SystemException, HeuristicRollbackException, HeuristicMixedException, RollbackException,
            NotSupportedException {
        Assertions.assertTrue(arjPropertyManager.getCoordinatorEnvironmentBean().isEnableStatistics());
        Assertions.assertEquals(60, recoveryPropertyManager.getRecoveryEnvironmentBean().getPeriodicRecoveryPeriod());

        TxStats stats = TxStats.getInstance();

        long cnt = stats.getNumberOfTransactions();

        tx.begin();
        tx.commit();

        Assertions.assertEquals(cnt + 1, stats.getNumberOfTransactions());
    }
}
