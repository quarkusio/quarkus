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

    /**
     * Enable the revision_on_collection_change feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_ON_COLLECTION_CHANGE}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean revisionOnCollectionChange;

    /**
     * Enable the do_not_audit_optimistic_locking_field feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean doNotAuditOptimisticLockingField;

    /**
     * Defines the default schema of where audit tables are to be created.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#DEFAULT_SCHEMA}.
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> defaultSchema;

    /**
     * Defines the default catalog of where audit tables are to be created.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#DEFAULT_CATALOG}.
     */
    @ConfigItem(defaultValue = "")
    public Optional<String> defaultCatalog;

    /**
     * Enables the track_entities_changed_in_revision feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#TRACK_ENTITIES_CHANGED_IN_REVISION}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean trackEntitiesChangedInRevision;

    /**
     * Enables the use_revision_entity_with_native_id feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#USE_REVISION_ENTITY_WITH_NATIVE_ID}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean useRevisionEntityWithNativeId;

    /**
     * Enables the global_with_modified_flag feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#GLOBAL_WITH_MODIFIED_FLAG}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean globalWithModifiedFlag;

    /**
     * Defines the suffix to be used for modified flag columns. Defaults to {@literal _MOD}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#MODIFIED_FLAG_SUFFIX}
     */
    @ConfigItem(defaultValue = "_MOD")
    public Optional<String> modifiedFlagSuffix;

    /**
     * Defines the fully qualified class name of a user defined revision listener.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_LISTENER}.
     */
    @ConfigItem
    public Optional<String> revisionListener;

    /**
     * Defines the fully qualified class name of the audit strategy to be used.
     *
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY}.
     */
    @ConfigItem(defaultValue = "org.hibernate.envers.strategy.DefaultAuditStrategy")
    public Optional<String> auditStrategy;

    /**
     * Defines the property name for the audit entity's composite primary key. Defaults to {@literal originalId}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#ORIGINAL_ID_PROP_NAME}.
     */
    @ConfigItem(defaultValue = "originalId")
    public Optional<String> originalIdPropName;

    /**
     * Defines the column name that holds the end revision number in audit entities. Defaults to {@literal REVEND}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME}.
     */
    @ConfigItem(defaultValue = "REVEND")
    public Optional<String> auditStrategyValidityEndRevFieldName;

    /**
     * Enables the audit_strategy_validity_store_revend_timestamp feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean auditStrategyValidityStoreRevendTimestamp;

    /**
     * Defines the column name of the revision end timestamp in the audit tables. Defaults to {@literal REVEND_TSTMP}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME}.
     */
    @ConfigItem(defaultValue = "REVEND_TSTMP")
    public Optional<String> auditStrategyValidityRevendTimestampFieldName;

    /**
     * Defines the name of the column used for storing collection ordinal values for embeddable elements.
     * Defaults to {@literal SETORDINAL}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#EMBEDDABLE_SET_ORDINAL_FIELD_NAME}.
     */
    @ConfigItem(defaultValue = "SETORDINAL")
    public Optional<String> embeddableSetOrdinalFieldName;

    /**
     * Enables the allow_identifier_reuse feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#ALLOW_IDENTIFIER_REUSE}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean allowIdentifierReuse;

    /**
     * Defines the naming strategy to be used for modified columns.
     * Defaults to {@literal org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#MODIFIED_COLUMN_NAMING_STRATEGY}.
     */
    @ConfigItem(defaultValue = "org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy")
    public Optional<String> modifiedColumnNamingStrategy;
}
