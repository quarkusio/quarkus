package io.quarkus.narayana.jta.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class TransactionManagerConfiguration {
    /**
     * The node name used by the transaction manager.
     * Must not exceed a length of 28 bytes.
     *
     * @see #shortenNodeNameIfNecessary
     */
    @ConfigItem(defaultValue = "quarkus")
    public String nodeName;

    /**
     * Whether the node name should be shortened if necessary.
     * The node name must not exceed a length of 28 bytes. If this property is set to {@code true}, and the node name exceeds 28
     * bytes, the node name is shortened by calculating the <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-224</a> hash,
     * which has a length of 28 bytes, encoded to Base64 format and then shorten to 28 bytes.
     *
     * @see #nodeName
     */
    @ConfigItem(defaultValue = "false")
    public boolean shortenNodeNameIfNecessary;

    /**
     * The default transaction timeout.
     */
    @ConfigItem(defaultValue = "60")
    public Duration defaultTransactionTimeout;

    /**
     * Start the recovery service on startup.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableRecovery;

    /**
     * The list of recovery modules.
     */
    @ConfigItem(defaultValue = "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule")
    public List<String> recoveryModules;

    /**
     * The list of expiry scanners.
     */
    @ConfigItem(defaultValue = "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner")
    public List<String> expiryScanners;

    /**
     * The list of orphan filters.
     */
    @ConfigItem(defaultValue = "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter")
    public List<String> xaResourceOrphanFilters;

    /**
     * The object store configuration.
     */
    @ConfigItem
    public ObjectStoreConfig objectStore;
}

@ConfigGroup
class ObjectStoreConfig {
    /**
     * The name of the directory where the transaction logs will be stored when using the {@code file-system} object store.
     * If the value is not absolute then the directory is relative
     * to the <em>user.dir</em> system property.
     */
    @ConfigItem(defaultValue = "ObjectStore")
    public String directory;

    /**
     * The type of object store.
     */
    @ConfigItem(defaultValue = "file-system")
    public ObjectStoreType type;

    /**
     * The name of the datasource where the transaction logs will be stored when using the {@code jdbc} object store.
     * <p>
     * If undefined, it will use the default datasource.
     */
    @ConfigItem
    public Optional<String> datasource = Optional.empty();

    /**
     * Whether to create the table if it does not exist.
     */
    @ConfigItem(defaultValue = "false")
    public boolean createTable;

    /**
     * Whether to drop the table on startup.
     */
    @ConfigItem(defaultValue = "false")
    public boolean dropTable;

    /**
     * The prefix to apply to the table.
     */
    @ConfigItem(defaultValue = "quarkus_")
    public String tablePrefix;
}
