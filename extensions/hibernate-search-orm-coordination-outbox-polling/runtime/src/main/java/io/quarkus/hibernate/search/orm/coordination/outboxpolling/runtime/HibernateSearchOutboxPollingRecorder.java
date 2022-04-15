package io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime;

import static io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime.HibernateSearchOutboxPollingConfigUtil.addCoordinationConfig;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime.HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit.AgentsConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchOutboxPollingRecorder {

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(
            HibernateSearchOutboxPollingRuntimeConfig runtimeConfig, String persistenceUnitName) {
        HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit puConfig = PersistenceUnitUtil
                .isDefaultPersistenceUnit(persistenceUnitName)
                        ? runtimeConfig.defaultPersistenceUnit
                        : runtimeConfig.persistenceUnits.get(persistenceUnitName);
        return new RuntimeInitListener(puConfig);
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

            contributeCoordinationRuntimeProperties(propertyCollector, null, runtimeConfig.coordination.defaults);

            for (Entry<String, AgentsConfig> tenantEntry : runtimeConfig.coordination.tenants.entrySet()) {
                contributeCoordinationRuntimeProperties(propertyCollector, tenantEntry.getKey(), tenantEntry.getValue());
            }
        }

        private void contributeCoordinationRuntimeProperties(BiConsumer<String, Object> propertyCollector, String tenantId,
                AgentsConfig agentsConfig) {
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_ENABLED,
                    agentsConfig.eventProcessor.enabled);
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_TOTAL_COUNT,
                    agentsConfig.eventProcessor.shards.totalCount);
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_ASSIGNED,
                    agentsConfig.eventProcessor.shards.assigned);
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_POLLING_INTERVAL,
                    agentsConfig.eventProcessor.pollingInterval.toMillis());
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_INTERVAL,
                    agentsConfig.eventProcessor.pulseInterval.toMillis());
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_EXPIRATION,
                    agentsConfig.eventProcessor.pulseExpiration.toMillis());
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_BATCH_SIZE,
                    agentsConfig.eventProcessor.batchSize);
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_TRANSACTION_TIMEOUT,
                    agentsConfig.eventProcessor.transactionTimeout, Optional::isPresent, d -> d.get().toSeconds());
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_RETRY_DELAY,
                    agentsConfig.eventProcessor.retryDelay.toSeconds());

            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_POLLING_INTERVAL,
                    agentsConfig.massIndexer.pollingInterval.toMillis());
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_INTERVAL,
                    agentsConfig.massIndexer.pulseInterval.toMillis());
            addCoordinationConfig(propertyCollector, tenantId,
                    HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_EXPIRATION,
                    agentsConfig.massIndexer.pulseExpiration.toMillis());
        }

    }
}
