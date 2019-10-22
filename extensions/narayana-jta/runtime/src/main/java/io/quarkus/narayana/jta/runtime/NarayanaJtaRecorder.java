package io.quarkus.narayana.jta.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaJtaRecorder {
    public void setNodeName(final TransactionManagerConfiguration transactions) {
        try {
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(transactions.nodeName);
            jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(Collections.singletonList(transactions.nodeName));
            TxControl.setXANodeName(transactions.nodeName);
        } catch (CoreEnvironmentBeanException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultTimeout(TransactionManagerConfiguration transactions) {
        transactions.defaultTransactionTimeout.ifPresent(defaultTimeout -> {
            arjPropertyManager.getCoordinatorEnvironmentBean().setDefaultTimeout((int) defaultTimeout.getSeconds());
            TxControl.setDefaultTimeout((int) defaultTimeout.getSeconds());
        });
    }

    public void disableTransactionStatusManager() {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setTransactionStatusManagerEnable(false);
    }

    private void setXaResourceOrphanFilterClassNames(JTAEnvironmentBean jtaEnvironmentBean) {
        jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(Arrays.asList(
                "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateJTAXAResourceOrphanFilter",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter"));
    }

    private void setRecoveryModuleClassNames(RecoveryEnvironmentBean recoveryEnvironmentBean) {
        recoveryEnvironmentBean.setRecoveryModuleClassNames(Arrays.asList(
                "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule",
                "com.arjuna.ats.internal.txoj.recovery.TORecoveryModule",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule",
                "com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule"));
    }

    private void setExpiryScannerClassNames(RecoveryEnvironmentBean recoveryEnvironmentBean) {
        recoveryEnvironmentBean.setExpiryScannerClassNames(Collections.singletonList(
                "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner"));
    }

    private void setRecoveryPort(RecoveryEnvironmentBean recoveryEnvironmentBean) {
        recoveryEnvironmentBean.setRecoveryPort(4712);
    }

    private void setRecoveryListener(RecoveryEnvironmentBean recoveryEnvironmentBean) {
        recoveryEnvironmentBean.setRecoveryListener(true);
    }

    public void setConfig(TransactionManagerConfiguration transactions) {
        Properties systemProperties;
        final SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            systemProperties = AccessController.doPrivileged((PrivilegedAction<Properties>) System::getProperties);
        } else {
            systemProperties = System.getProperties();
        }

        // directly set the Narayana property beans to avoid the need to parse property files
        if (transactions.checkForNarayanaSystemProperties) {
            RecoveryEnvironmentBean recoveryEnvironmentBean;
            JTAEnvironmentBean jtaEnvironmentBean;

            BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class, systemProperties);
            BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class, systemProperties);
            BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class, systemProperties);

            recoveryEnvironmentBean = BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class, systemProperties);
            jtaEnvironmentBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class, systemProperties);

            // set defaults if not set via system properties
            if (!systemProperties.containsKey("JTAEnvironmentBean.xaResourceOrphanFilterClassNames")) {
                setXaResourceOrphanFilterClassNames(jtaEnvironmentBean);
            }

            if (!systemProperties.containsKey("RecoveryEnvironmentBean.recoveryModuleClassNames")) {
                setRecoveryModuleClassNames(recoveryEnvironmentBean);
            }

            if (!systemProperties.containsKey("RecoveryEnvironmentBean.expiryScannerClassNames")) {
                setExpiryScannerClassNames(recoveryEnvironmentBean);
            }

            if (!systemProperties.containsKey("RecoveryEnvironmentBean.recoveryPort")) {
                setRecoveryPort(recoveryEnvironmentBean);
            }

            if (!systemProperties.containsKey("RecoveryEnvironmentBean.recoveryListener")) {
                setRecoveryListener(recoveryEnvironmentBean);
            }
        } else {
            RecoveryEnvironmentBean recoveryEnvironmentBean = new RecoveryEnvironmentBean();
            JTAEnvironmentBean jtaEnvironmentBean = new JTAEnvironmentBean();
            CoreEnvironmentBean coreEnvironmentBean = new CoreEnvironmentBean();
            CoordinatorEnvironmentBean coordinatorEnvironmentBean = new CoordinatorEnvironmentBean();
            ObjectStoreEnvironmentBean objectStoreEnvironmentBean = new ObjectStoreEnvironmentBean();

            setXaResourceOrphanFilterClassNames(jtaEnvironmentBean);
            setRecoveryModuleClassNames(recoveryEnvironmentBean);
            setExpiryScannerClassNames(recoveryEnvironmentBean);
            setRecoveryPort(recoveryEnvironmentBean);
            setRecoveryListener(recoveryEnvironmentBean);

            BeanPopulator.setBeanInstanceIfAbsent(CoordinatorEnvironmentBean.class.getName(), coordinatorEnvironmentBean);
            BeanPopulator.setBeanInstanceIfAbsent(RecoveryEnvironmentBean.class.getName(), recoveryEnvironmentBean);
            BeanPopulator.setBeanInstanceIfAbsent(CoreEnvironmentBean.class.getName(), coreEnvironmentBean);
            BeanPopulator.setBeanInstanceIfAbsent(JTAEnvironmentBean.class.getName(), jtaEnvironmentBean);
            BeanPopulator.setBeanInstanceIfAbsent(ObjectStoreEnvironmentBean.class.getName(), objectStoreEnvironmentBean);
        }

        // This must be done before setNodeName as the code in setNodeName will create a TSM based on the value of this
        disableTransactionStatusManager();
        setNodeName(transactions);
        setDefaultTimeout(transactions);

        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(transactions.objectStoreDir);
    }
}
