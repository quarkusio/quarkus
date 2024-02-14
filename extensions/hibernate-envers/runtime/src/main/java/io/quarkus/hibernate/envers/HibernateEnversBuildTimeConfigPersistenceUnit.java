package io.quarkus.hibernate.envers;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface HibernateEnversBuildTimeConfigPersistenceUnit {

    /**
     * Whether Hibernate Envers should be active for this persistence unit at runtime.
     *
     * If Hibernate Envers is not active, the audit entities will *still* be added to the Hibernate ORM metamodel
     * and to the database schema that Hibernate ORM generates:
     * you would need to disable Hibernate Envers at build time (i.e. set `quarkus.hibernate-envers.enabled` to `false`)
     * in order to avoid that.
     * However, when Hibernate Envers is not active, it will not process entity change events
     * nor create new versions of entities.
     * and accessing the AuditReader through AuditReaderFactory will not be possible.
     *
     * Note that if Hibernate Envers is disabled (i.e. `quarkus.hibernate-envers.enabled` is set to `false`),
     * it won't be active for any persistence unit, and setting this property to `true` will fail.
     *
     * @asciidoclet
     */
    @ConfigDocDefault("'true' if Hibernate ORM is enabled; 'false' otherwise")
    Optional<Boolean> active();

    /**
     * Enable store_data_at_delete feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#STORE_DATA_AT_DELETE}.
     */
    @WithDefault("false")
    boolean storeDataAtDelete();

    /**
     * Defines a suffix for historical data table. Defaults to {@literal _AUD}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_TABLE_SUFFIX}.
     */
    @WithDefault("_AUD")
    Optional<String> auditTableSuffix();

    /**
     * Defines a prefix for historical data table. Default is the empty string.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_TABLE_PREFIX}.
     */
    @WithDefault("")
    Optional<String> auditTablePrefix();

    /**
     * Revision field name. Defaults to {@literal REV}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_FIELD_NAME}.
     */
    @WithDefault("REV")
    Optional<String> revisionFieldName();

    /**
     * Revision type field name. Defaults to {@literal REVTYPE}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_TYPE_FIELD_NAME}.
     */
    @WithDefault("REVTYPE")
    Optional<String> revisionTypeFieldName();

    /**
     * Enable the revision_on_collection_change feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_ON_COLLECTION_CHANGE}.
     */
    @WithDefault("true")
    boolean revisionOnCollectionChange();

    /**
     * Enable the do_not_audit_optimistic_locking_field feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD}.
     */
    @WithDefault("true")
    boolean doNotAuditOptimisticLockingField();

    /**
     * Defines the default schema of where audit tables are to be created.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#DEFAULT_SCHEMA}.
     */
    @WithDefault("")
    Optional<String> defaultSchema();

    /**
     * Defines the default catalog of where audit tables are to be created.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#DEFAULT_CATALOG}.
     */
    @WithDefault("")
    Optional<String> defaultCatalog();

    /**
     * Enables the track_entities_changed_in_revision feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#TRACK_ENTITIES_CHANGED_IN_REVISION}.
     */
    @WithDefault("false")
    boolean trackEntitiesChangedInRevision();

    /**
     * Enables the use_revision_entity_with_native_id feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#USE_REVISION_ENTITY_WITH_NATIVE_ID}.
     */
    @WithDefault("true")
    boolean useRevisionEntityWithNativeId();

    /**
     * Enables the global_with_modified_flag feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#GLOBAL_WITH_MODIFIED_FLAG}.
     */
    @WithDefault("false")
    boolean globalWithModifiedFlag();

    /**
     * Defines the suffix to be used for modified flag columns. Defaults to {@literal _MOD}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#MODIFIED_FLAG_SUFFIX}
     */
    @WithDefault("_MOD")
    Optional<String> modifiedFlagSuffix();

    /**
     * Defines the fully qualified class name of a user defined revision listener.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#REVISION_LISTENER}.
     */
    Optional<String> revisionListener();

    /**
     * Defines the fully qualified class name of the audit strategy to be used.
     *
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY}.
     */
    @WithDefault("org.hibernate.envers.strategy.DefaultAuditStrategy")
    Optional<String> auditStrategy();

    /**
     * Defines the property name for the audit entity's composite primary key. Defaults to {@literal originalId}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#ORIGINAL_ID_PROP_NAME}.
     */
    @WithDefault("originalId")
    Optional<String> originalIdPropName();

    /**
     * Defines the column name that holds the end revision number in audit entities. Defaults to {@literal REVEND}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME}.
     */
    @WithDefault("REVEND")
    Optional<String> auditStrategyValidityEndRevFieldName();

    /**
     * Enables the audit_strategy_validity_store_revend_timestamp feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP}.
     */
    @WithDefault("false")
    boolean auditStrategyValidityStoreRevendTimestamp();

    /**
     * Defines the column name of the revision end timestamp in the audit tables. Defaults to {@literal REVEND_TSTMP}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME}.
     */
    @WithDefault("REVEND_TSTMP")
    Optional<String> auditStrategyValidityRevendTimestampFieldName();

    /**
     * Defines the name of the column used for storing collection ordinal values for embeddable elements.
     * Defaults to {@literal SETORDINAL}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#EMBEDDABLE_SET_ORDINAL_FIELD_NAME}.
     */
    @WithDefault("SETORDINAL")
    Optional<String> embeddableSetOrdinalFieldName();

    /**
     * Enables the allow_identifier_reuse feature.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#ALLOW_IDENTIFIER_REUSE}.
     */
    @WithDefault("false")
    boolean allowIdentifierReuse();

    /**
     * Defines the naming strategy to be used for modified columns.
     * Defaults to {@literal org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy}.
     * Maps to {@link org.hibernate.envers.configuration.EnversSettings#MODIFIED_COLUMN_NAMING_STRATEGY}.
     */
    @WithDefault("org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy")
    Optional<String> modifiedColumnNamingStrategy();

}
