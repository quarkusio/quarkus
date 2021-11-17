package io.quarkus.hibernate.envers;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateEnversBuildTimeConfig {
    /**
     * Enable store_data_at_delete feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#STORE_DATA_AT_DELETE}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean storeDataAtDelete;

    /**
     * Defines a suffix for historical data table. Defaults to {@literal _AUD}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_TABLE_SUFFIX}.
     */
    @ConfigItem(defaultValue = "_AUD")
    public Optional<String> auditTableSuffix;

    /**
     * Defines a prefix for historical data table. Default is the empty string.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_TABLE_PREFIX}.
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> auditTablePrefix;

    /**
     * Revision field name. Defaults to {@literal REV}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_FIELD_NAME}.
     */
    @ConfigItem(defaultValue = "REV")
    public Optional<String> revisionFieldName;

    /**
     * Revision type field name. Defaults to {@literal REVTYPE}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_TYPE_FIELD_NAME}.
     */
    @ConfigItem(defaultValue = "REVTYPE")
    public Optional<String> revisionTypeFieldName;

}
