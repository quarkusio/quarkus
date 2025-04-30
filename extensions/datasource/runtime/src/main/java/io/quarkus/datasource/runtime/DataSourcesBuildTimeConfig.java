package io.quarkus.datasource.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DataSourcesBuildTimeConfig {

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    Map<String, DataSourceBuildTimeConfig> dataSources();

    /**
     * Whether or not a health check is published in case the smallrye-health extension is present.
     * <p>
     * This is a global setting and is not specific to a datasource.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    /**
     * Whether or not datasource metrics are published in case a metrics extension is present.
     * <p>
     * This is a global setting and is not specific to a datasource.
     * <p>
     * NOTE: This is different from the "jdbc.enable-metrics" property that needs to be set on the JDBC datasource level to
     * enable collection of metrics for that datasource.
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * Only here to detect configuration errors.
     * <p>
     * This used to be runtime but we don't really care, we just want to catch invalid configurations.
     *
     * @deprecated
     */
    @Deprecated
    Optional<String> url();

    /**
     * Only here to detect configuration errors.
     *
     * @deprecated
     */
    @Deprecated
    Optional<String> driver();

    default boolean hasNamedDataSources() {
        return dataSources().keySet().size() > 1
                || (!dataSources().isEmpty()
                        && !dataSources().containsKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME));
    }
}
