package io.quarkus.narayana.jta.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.transaction-manager")
public interface TransactionManagerBuildTimeConfig {
    /**
     * Define the behavior when using multiple XA unaware resources in the same transactional demarcation.
     * <p>
     * Defaults to {@code fail}.
     * {@code warn-each}, {@code warn-first}, and {@code allow} are UNSAFE and should only be used for compatibility.
     * Either use XA for all resources if you want consistency, or split the code into separate
     * methods with separate transactions.
     * <p>
     * Note that using a single XA unaware resource together with XA aware resources, known as
     * the Last Resource Commit Optimization (LRCO), is different from using multiple XA unaware
     * resources. Although LRCO allows most transactions to complete normally, some errors can
     * cause an inconsistent transaction outcome. Using multiple XA unaware resources is not
     * recommended since the probability of inconsistent outcomes is significantly higher and
     * much harder to recover from than LRCO. For this reason, use LRCO as a last resort.
     * <p>
     * We do not recommend using this configuration property, and we plan to remove it in the future,
     * so you should plan fixing your application accordingly.
     * If you think your use case of this feature is valid and this option should be kept around,
     * open an issue in our tracker explaining why.
     *
     * @deprecated This property is planned for removal in a future version.
     */
    @Deprecated(forRemoval = true)
    @ConfigDocDefault("fail")
    public Optional<UnsafeMultipleLastResourcesMode> unsafeMultipleLastResources();

    public enum UnsafeMultipleLastResourcesMode {
        /**
         * Allow using multiple XA unaware resources in the same transactional demarcation.
         * <p>
         * This will log a warning once on application startup,
         * but not on each use of multiple XA unaware resources in the same transactional demarcation.
         */
        ALLOW,
        /**
         * Allow using multiple XA unaware resources in the same transactional demarcation,
         * but log a warning on the first occurrence.
         */
        WARN_FIRST,
        /**
         * Allow using multiple XA unaware resources in the same transactional demarcation,
         * but log a warning on each occurrence.
         */
        WARN_EACH,
        /**
         * Allow using multiple XA unaware resources in the same transactional demarcation,
         * but log a warning on each occurrence.
         */
        FAIL;

        // The default is WARN_FIRST in Quarkus 3.8, FAIL in Quarkus 3.9+
        // Make sure to update defaultValueDocumentation on unsafeMultipleLastResources when changing this.
        public static final UnsafeMultipleLastResourcesMode DEFAULT = FAIL;
    }

}
