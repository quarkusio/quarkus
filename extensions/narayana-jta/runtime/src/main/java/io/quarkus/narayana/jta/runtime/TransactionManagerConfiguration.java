package io.quarkus.narayana.jta.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class TransactionManagerConfiguration {
    /**
     * The node name used by the transaction manager
     */
    @ConfigItem(defaultValue = "quarkus")
    public String nodeName;

    /**
     * The default transaction timeout in seconds
     */
    @ConfigItem(defaultValue = "300")
    public Optional<Duration> defaultTransactionTimeout;

    /**
     * The directory name of location of the transaction logs.
     * If the value is not absolute then the directory is relative
     * to the <em>user.dir</em> system property.
     */
    @ConfigItem(name = "objectStoreDir", defaultValue = "ObjectStore")
    public String objectStoreDir;

    /**
     * <p>
     * The JTA extension uses the Narayana transaction manager as the underlying transaction engine.
     * There are many configuration options referenced in the
     * <a href="https://narayana.io/docs/project/index.html">Narayana documentation guide</a>.
     * The canonical reference for these configuration options is in the Javadoc for the various
     * EnvironmentBean classes. For example the javadoc for the transaction log configuration would be in the
     * <a href="https://narayana.io/docs/api/com/arjuna/ats/arjuna/common/ObjectStoreEnvironmentBeanMBean.html">
     * ObjectStoreEnvironmentBean</a> class. Currently any of the following beans may be configured via this
     * mechanism: RecoveryEnvironmentBean, JTAEnvironmentBean, CoreEnvironmentBean, CoordinatorEnvironmentBean
     * and ObjectStoreEnvironmentBean.
     * </p>
     * <p>
     * To enable full access to any config option provided by these environment beans set this quarkus
     * configuration item to true. The extension will then check whether or not there are any java
     * system properties of the form "environment bean name"."property name" and if present will use
     * that value to configure the transaction manager.
     * </p>
     * <p>
     * If any Narayana transaction manager config property is not set via a system property then
     * the extension will assume the following default values for the bean properties:
     * </p>
     * <p>
     * <table style="width:100%">
     * <tr>
     * <th>System Property</th>
     * <th>Value</th>
     * </tr>
     * <tr>
     * <td>JTAEnvironmentBean.xaResourceOrphanFilterClassNames</td>
     * <td>com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter,
     * com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter,
     * com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateJTAXAResourceOrphanFilter,
     * com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter
     * </td>
     * </tr>
     * <tr>
     * <td>RecoveryEnvironmentBean.recoveryModuleClassNames</td>
     * <td>com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule,
     * com.arjuna.ats.internal.txoj.recovery.TORecoveryModule,
     * com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule,
     * com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule
     * com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule
     * </td>
     * </tr>
     * <tr>
     * <td>RecoveryEnvironmentBean.expiryScannerClassNames</td>
     * <td>com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner
     * </td>
     * <tr>
     * <td>RecoveryEnvironmentBean.recoveryPort</td>
     * <td>4712</td>
     * </tr>
     * <tr>
     * <td>RecoveryEnvironmentBean.recoveryListener</td>
     * <td>true</td>
     * </tr>
     * </table>
     * </p>
     * <p>
     * The following two Narayana config properties cannot be set in this way:
     * </p>
     * <p>
     * <em>JTAEnvironmentBean.xaRecoveryNodes</em> which is always initialised to the value of
     * {@link TransactionManagerConfiguration#nodeName}
     * </p>
     * <p>
     * <em></em>CoordinatorEnvironmentBean.transactionStatusManagerEnable</em> which is always initialised to the value false
     * </p>
     * <p>
     * Note that quarkus config properties will override Narayana system properties.
     * </p>
     */
    @ConfigItem(name = "checkForNarayanaSystemProperties", defaultValue = "false")
    public boolean checkForNarayanaSystemProperties;
}
