package io.quarkus.narayana.jta.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.transaction-manager")
public interface TransactionManagerConfiguration {
    /**
     * The node name used by the transaction manager.
     * Must not exceed a length of 28 bytes.
     *
     * @see #shortenNodeNameIfNecessary
     */
    @WithDefault("quarkus")
    String nodeName();

    /**
     * Whether the node name should be shortened if necessary.
     * The node name must not exceed a length of 28 bytes. If this property is set to {@code true}, and the node name exceeds 28
     * bytes, the node name is shortened by calculating the <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-224</a> hash,
     * which has a length of 28 bytes, encoded to Base64 format and then shortened to 28 bytes.
     *
     * @see #nodeName
     */
    @WithDefault("false")
    boolean shortenNodeNameIfNecessary();

    /**
     * The default transaction timeout.
     */
    @WithDefault("60")
    Duration defaultTransactionTimeout();

    /**
     * Start the recovery service on startup.
     * <p>
     * If not set, the recovery service will be started automatically if XA datasources are configured.
     * Set to {@code true} to always enable recovery, or {@code false} to explicitly disable it.
     */
    @ConfigDocDefault("`true` if XA datasources are configured, `false` otherwise")
    Optional<Boolean> enableRecovery();

    /**
     * The list of recovery modules.
     */
    @WithDefault("com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule")
    List<String> recoveryModules();

    /**
     * The list of expiry scanners.
     */
    @WithDefault("com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner")
    List<String> expiryScanners();

    /**
     * The list of orphan filters.
     */
    @WithDefault("com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter," +
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter")
    List<String> xaResourceOrphanFilters();

    /**
     * The transaction reaper configuration.
     * <p>
     * The transaction reaper is a background thread that monitors running transactions and cancels
     * those that exceed their timeout. These settings control how aggressively it acts.
     */
    ReaperConfig reaper();

    /**
     * The object store configuration.
     */
    ObjectStoreConfig objectStore();

    @ConfigGroup
    interface ReaperConfig {
        /**
         * The interval at which the reaper checks for timed-out transactions.
         */
        @WithDefault("120s")
        Duration checkPeriod();

        /**
         * The time the reaper waits before interrupting a cancel worker that is rolling back
         * a timed-out transaction.
         * <p>
         * When a transaction exceeds its timeout, the reaper schedules a cancel and waits this period before
         * interrupting the cancel worker. In CPU-constrained environments (e.g., containers with strict CPU limits),
         * the default of 500ms may be too aggressive, causing premature zombie transaction declarations.
         */
        @WithDefault("500ms")
        Duration cancelWaitPeriod();

        /**
         * The time the reaper waits after interrupting a cancel worker before declaring the
         * transaction a zombie.
         * <p>
         * After interrupting a cancel worker, the reaper waits this period. If the worker has not completed
         * by then, the transaction is marked as a zombie. In CPU-constrained environments, increasing this
         * value gives the application thread more time to respond to the cancellation.
         */
        @WithDefault("500ms")
        Duration cancelFailWaitPeriod();

        /**
         * The maximum number of zombie transactions before the reaper escalates to ERROR-level logging.
         * <p>
         * A zombie transaction is one where the cancel worker was unable to roll it back within the
         * configured wait periods. Once this threshold is reached, subsequent zombies are logged at
         * ERROR level instead of WARN.
         */
        @WithDefault("8")
        int zombieMax();
    }

    @ConfigGroup
    public interface ObjectStoreConfig {
        /**
         * The name of the directory where the transaction logs will be stored when using the {@code file-system} object store.
         * If the value is not absolute then the directory is relative
         * to the <em>user.dir</em> system property.
         */
        @WithDefault("ObjectStore")
        String directory();

        /**
         * The type of object store.
         */
        @WithDefault("file-system")
        ObjectStoreType type();

        /**
         * The name of the datasource where the transaction logs will be stored when using the {@code jdbc} object store.
         * <p>
         * If undefined, it will use the default datasource.
         */
        Optional<String> datasource();

        /**
         * Whether to create the table if it does not exist.
         */
        @WithDefault("false")
        boolean createTable();

        /**
         * Whether to drop the table on startup.
         */
        @WithDefault("false")
        boolean dropTable();

        /**
         * The prefix to apply to the table.
         */
        @WithDefault("quarkus_")
        String tablePrefix();
    }
}
