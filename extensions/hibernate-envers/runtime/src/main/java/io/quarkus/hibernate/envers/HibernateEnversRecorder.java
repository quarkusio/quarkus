package io.quarkus.hibernate.envers;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.envers.configuration.EnversSettings;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateEnversRecorder {

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
        return new HibernateEnversIntegrationListener(buildTimeConfig);
    }

    private static final class HibernateEnversIntegrationListener implements HibernateOrmIntegrationStaticInitListener {
        private HibernateEnversBuildTimeConfig buildTimeConfig;

        private HibernateEnversIntegrationListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector, EnversSettings.STORE_DATA_AT_DELETE, buildTimeConfig.storeDataAtDelete);
            addConfig(propertyCollector, EnversSettings.AUDIT_TABLE_SUFFIX, buildTimeConfig.auditTableSuffix);
            addConfig(propertyCollector, EnversSettings.AUDIT_TABLE_PREFIX, buildTimeConfig.auditTablePrefix);
            addConfig(propertyCollector, EnversSettings.REVISION_FIELD_NAME, buildTimeConfig.revisionFieldName);
            addConfig(propertyCollector, EnversSettings.REVISION_TYPE_FIELD_NAME, buildTimeConfig.revisionTypeFieldName);
            addConfig(propertyCollector, EnversSettings.REVISION_ON_COLLECTION_CHANGE,
                    buildTimeConfig.revisionOnCollectionChange);
            addConfig(propertyCollector, EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD,
                    buildTimeConfig.doNotAuditOptimisticLockingField);
            addConfig(propertyCollector, EnversSettings.DEFAULT_SCHEMA, buildTimeConfig.defaultSchema);
            addConfig(propertyCollector, EnversSettings.DEFAULT_CATALOG, buildTimeConfig.defaultCatalog);
            addConfig(propertyCollector, EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION,
                    buildTimeConfig.trackEntitiesChangedInRevision);
            addConfig(propertyCollector, EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID,
                    buildTimeConfig.useRevisionEntityWithNativeId);
            addConfig(propertyCollector, EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, buildTimeConfig.globalWithModifiedFlag);
            addConfig(propertyCollector, EnversSettings.MODIFIED_FLAG_SUFFIX, buildTimeConfig.modifiedFlagSuffix);
            addConfigIfPresent(propertyCollector, EnversSettings.REVISION_LISTENER, buildTimeConfig.revisionListener);
            addConfigIfPresent(propertyCollector, EnversSettings.AUDIT_STRATEGY, buildTimeConfig.auditStrategy);
            addConfigIfPresent(propertyCollector, EnversSettings.ORIGINAL_ID_PROP_NAME, buildTimeConfig.originalIdPropName);
            addConfigIfPresent(propertyCollector, EnversSettings.AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME,
                    buildTimeConfig.auditStrategyValidityEndRevFieldName);
            addConfig(propertyCollector, EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP,
                    buildTimeConfig.auditStrategyValidityStoreRevendTimestamp);
            addConfigIfPresent(propertyCollector, EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME,
                    buildTimeConfig.auditStrategyValidityRevendTimestampFieldName);
            addConfigIfPresent(propertyCollector, EnversSettings.EMBEDDABLE_SET_ORDINAL_FIELD_NAME,
                    buildTimeConfig.embeddableSetOrdinalFieldName);
            addConfig(propertyCollector, EnversSettings.ALLOW_IDENTIFIER_REUSE, buildTimeConfig.allowIdentifierReuse);
            addConfigIfPresent(propertyCollector, EnversSettings.MODIFIED_COLUMN_NAMING_STRATEGY,
                    buildTimeConfig.modifiedColumnNamingStrategy);
        }

        public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, T value) {
            propertyCollector.accept(configPath, value);
        }

        public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, Optional<T> value) {
            if (value.isPresent()) {
                propertyCollector.accept(configPath, value.get());
            } else {
                propertyCollector.accept(configPath, "");
            }
        }

        public static <T> void addConfigIfPresent(BiConsumer<String, Object> propertyCollector, String configPath,
                Optional<T> value) {
            value.ifPresent(t -> propertyCollector.accept(configPath, t));
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }
    }
}
