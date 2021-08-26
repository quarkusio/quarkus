package io.quarkus.hibernate.envers;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateEnversBuildTimeConfig {
    /**
     * Enable store_data_at_delete feature.
     */
    @ConfigItem(defaultValue = "false")
    public boolean storeDataAtDelete;

    /**
     * Defines a suffix for historical data table.
     */
    @ConfigItem(defaultValue = "_AUD")
    public boolean auditTableSuffix;

}
