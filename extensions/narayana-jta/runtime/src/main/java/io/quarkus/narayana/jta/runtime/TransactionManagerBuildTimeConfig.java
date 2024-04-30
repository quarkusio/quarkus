package io.quarkus.narayana.jta.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public final class TransactionManagerBuildTimeConfig {
    /**
     * Allow using multiple XA unaware resources in the same transactional demarcation.
     * This is UNSAFE and may only be used for compatibility.
     * <p>
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
     * We plan to remove this property in the future so you should plan fixing your application
     * accordingly.
     * If you think your use case of this feature is valid and this option should be kept around,
     * open an issue in our tracker explaining why.
     *
     * @deprecated This property is planned for removal in a future version.
     */
    @Deprecated(forRemoval = true)
    @ConfigItem(defaultValue = "false")
    public boolean allowUnsafeMultipleLastResources;

}