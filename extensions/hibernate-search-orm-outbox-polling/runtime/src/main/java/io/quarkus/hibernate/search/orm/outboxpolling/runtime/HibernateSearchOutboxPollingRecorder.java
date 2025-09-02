package io.quarkus.hibernate.search.orm.outboxpolling.runtime;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchOutboxPollingRecorder {
    private final HibernateSearchOutboxPollingBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<HibernateSearchOutboxPollingRuntimeConfig> runtimeConfig;

    public HibernateSearchOutboxPollingRecorder(
            final HibernateSearchOutboxPollingBuildTimeConfig buildTimeConfig,
            final RuntimeValue<HibernateSearchOutboxPollingRuntimeConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(String persistenceUnitName) {
        HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit puConfig = buildTimeConfig.persistenceUnits()
                .get(persistenceUnitName);
        return new StaticInitListener(puConfig);
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(String persistenceUnitName) {
        HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit puConfig = runtimeConfig.getValue().persistenceUnits()
                .get(persistenceUnitName);
        return new RuntimeInitListener(puConfig);
    }

    private static final class StaticInitListener
            implements HibernateOrmIntegrationStaticInitListener {

        private final HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit buildTimeConfig;

        private StaticInitListener(HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
            // Nothing to do
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            if (buildTimeConfig == null) {
                return;
            }

            contributeCoordinationBuildTimeProperties(propertyCollector, buildTimeConfig.coordination());
        }

        private void contributeCoordinationBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
                HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit.CoordinationConfig config) {
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_CATALOG,
                    config.entityMapping().agent().catalog());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_SCHEMA,
                    config.entityMapping().agent().schema());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_TABLE,
                    config.entityMapping().agent().table());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY,
                    config.entityMapping().agent().uuidGenStrategy());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_AGENT_UUID_TYPE,
                    config.entityMapping().agent().uuidType());

            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_CATALOG,
                    config.entityMapping().outboxEvent().catalog());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_SCHEMA,
                    config.entityMapping().outboxEvent().schema());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_TABLE,
                    config.entityMapping().outboxEvent().table());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY,
                    config.entityMapping().outboxEvent().uuidGenStrategy());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE,
                    config.entityMapping().outboxEvent().uuidType());
        }

    }

    private static final class RuntimeInitListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private final HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit runtimeConfig;

        private RuntimeInitListener(HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (runtimeConfig == null) {
                return;
            }

            contributeCoordinationRuntimeProperties(propertyCollector, null, runtimeConfig.coordination().defaults());

            for (Entry<String, HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit.AgentsConfig> tenantEntry : runtimeConfig
                    .coordination().tenants().entrySet()) {
                contributeCoordinationRuntimeProperties(propertyCollector, tenantEntry.getKey(), tenantEntry.getValue());
            }
        }

        private void contributeCoordinationRuntimeProperties(BiConsumer<String, Object> propertyCollector, String tenantId,
                HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit.AgentsConfig agentsConfig) {
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_ENABLED,
                    agentsConfig.eventProcessor().enabled());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_TOTAL_COUNT,
                    agentsConfig.eventProcessor().shards().totalCount());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_ASSIGNED,
                    agentsConfig.eventProcessor().shards().assigned());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_POLLING_INTERVAL,
                    agentsConfig.eventProcessor().pollingInterval().toMillis());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_INTERVAL,
                    agentsConfig.eventProcessor().pulseInterval().toMillis());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_EXPIRATION,
                    agentsConfig.eventProcessor().pulseExpiration().toMillis());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_BATCH_SIZE,
                    agentsConfig.eventProcessor().batchSize());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_TRANSACTION_TIMEOUT,
                    agentsConfig.eventProcessor().transactionTimeout(), Optional::isPresent, d -> d.get().toSeconds());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_RETRY_DELAY,
                    agentsConfig.eventProcessor().retryDelay().toSeconds());

            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_POLLING_INTERVAL,
                    agentsConfig.massIndexer().pollingInterval().toMillis());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_INTERVAL,
                    agentsConfig.massIndexer().pulseInterval().toMillis());
            HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_EXPIRATION,
                    agentsConfig.massIndexer().pulseExpiration().toMillis());
        }

    }
}
