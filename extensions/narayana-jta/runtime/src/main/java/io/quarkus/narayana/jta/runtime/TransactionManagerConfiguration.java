package io.quarkus.narayana.jta.runtime;

import java.time.Duration;
import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class TransactionManagerConfiguration {
    /**
     * The node name used by the transaction manager
     */
    @ConfigItem(defaultValue = "quarkus")
    public String nodeName;

    /**
     * The default transaction timeout
     */
    @ConfigItem(defaultValue = "60")
    public Duration defaultTransactionTimeout;

    /**
     * The directory name of location of the transaction logs.
     * If the value is not absolute then the directory is relative
     * to the <em>user.dir</em> system property.
     */
    @ConfigItem(defaultValue = "ObjectStore")
    public String objectStoreDirectory;

    /**
     * Start the recovery service on startup.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableRecovery;

    /**
     * The list of recovery modules
     */
    @ConfigItem(defaultValue = "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule")
    public List<String> recoveryModules;

    /**
     * The list of expiry scanners
     */
    @ConfigItem(defaultValue = "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner")
    public List<String> expiryScanners;

    /**
     * The list of orphan filters
     */
    @ConfigItem(defaultValue = "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter")
    public List<String> xaResourceOrphanFilters;
}
