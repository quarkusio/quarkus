package io.quarkus.hibernate.search.orm.coordination.outboxpolling.test.configuration;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.search.orm.coordination.outboxpolling.test.configuration.defaultpu.IndexedEntity;
import io.quarkus.hibernate.search.orm.coordination.outboxpolling.test.configuration.pu1.IndexedEntityForPU1;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that configuration properties set in Quarkus are translated to the right key and value in Hibernate Search.
 */
public class ConfigPropertiesTest {

    static final String TENANT_ID = "my-tenant";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(IndexedEntity.class.getPackage())
                    .addPackage(IndexedEntityForPU1.class.getPackage()))
            .withConfigurationResource("application-multiple-persistence-units.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.shards.total-count", "10")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.shards.assigned", "1,2")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.polling-interval", "0.042S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.pulse-interval", "0.043S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.pulse-expiration", "44S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.batch-size", "45")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.transaction-timeout", "46S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.event-processor.retry-delay", "47S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.mass-indexer.polling-interval", "0.048S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.mass-indexer.pulse-interval", "0.049S")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.mass-indexer.pulse-expiration", "50S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.shards.total-count", "110")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.shards.assigned", "11,12")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.polling-interval", "0.142S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.pulse-interval", "0.143S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.pulse-expiration", "144S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.batch-size", "145")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.transaction-timeout", "146S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.event-processor.retry-delay", "147S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.mass-indexer.polling-interval", "0.148S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.mass-indexer.pulse-interval", "0.149S")
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu1\".coordination.mass-indexer.pulse-expiration", "150S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID
                    + "\".event-processor.shards.total-count", "210")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.shards.assigned",
                    "21,22")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.polling-interval",
                    "0.242S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.pulse-interval",
                    "0.243S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.pulse-expiration",
                    "244S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.batch-size", "245")
            .overrideConfigKey("quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID
                    + "\".event-processor.transaction-timeout", "246S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".event-processor.retry-delay",
                    "247S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".mass-indexer.polling-interval",
                    "0.248S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".mass-indexer.pulse-interval",
                    "0.249S")
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.coordination.tenants.\"" + TENANT_ID + "\".mass-indexer.pulse-expiration",
                    "250S");

    @Inject
    SessionFactory sessionFactory;

    @Inject
    @PersistenceUnit("pu1")
    SessionFactory sessionFactoryForNamedPU1;

    @Test
    public void root() {
        assertThat(sessionFactory.getProperties())
                .contains(
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_ENABLED, false),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT, 10),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED,
                                List.of(1, 2)),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL, 42L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL, 43L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION, 44000L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_BATCH_SIZE, 45),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT, 46L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_RETRY_DELAY, 47L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_MASS_INDEXER_POLLING_INTERVAL, 48L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_MASS_INDEXER_PULSE_INTERVAL, 49L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_MASS_INDEXER_PULSE_EXPIRATION, 50000L));
    }

    @Test
    public void perNamedPersistenceUnit() {
        assertThat(sessionFactoryForNamedPU1.getProperties())
                .contains(
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_ENABLED, false),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT, 110),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED,
                                List.of(11, 12)),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL, 142L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL, 143L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION, 144000L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_BATCH_SIZE, 145),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT, 146L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_RETRY_DELAY, 147L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_MASS_INDEXER_POLLING_INTERVAL, 148L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_MASS_INDEXER_PULSE_INTERVAL, 149L),
                        entry(HibernateOrmMapperOutboxPollingSettings.COORDINATION_MASS_INDEXER_PULSE_EXPIRATION, 150000L));
    }

    @Test
    public void perTenant() {
        assertThat(sessionFactory.getProperties())
                .contains(
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_ENABLED), false),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_TOTAL_COUNT),
                                210),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_ASSIGNED),
                                List.of(21, 22)),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_POLLING_INTERVAL),
                                242L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_INTERVAL),
                                243L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_EXPIRATION),
                                244000L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_BATCH_SIZE), 245),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_TRANSACTION_TIMEOUT),
                                246L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_RETRY_DELAY),
                                247L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_POLLING_INTERVAL),
                                248L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_INTERVAL),
                                249L),
                        entry(HibernateOrmMapperOutboxPollingSettings.coordinationKey(TENANT_ID,
                                HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_EXPIRATION),
                                250000L));
    }

}
