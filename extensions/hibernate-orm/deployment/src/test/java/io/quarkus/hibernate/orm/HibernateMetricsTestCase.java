package io.quarkus.hibernate.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.transaction.Transactional;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Smoke test for metrics exposed by a metrics extension and computed from Hibernate statistics objects.
 */
public class HibernateMetricsTestCase {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource("application-metrics-enabled.properties", "application.properties")
            .addClasses(DummyEntity.class));

    @Entity(name = "DummyEntity")
    static class DummyEntity {

        @Id
        private Long number;

        public Long getNumber() {
            return number;
        }

        public void setNumber(Long number) {
            this.number = number;
        }
    }

    @Inject
    EntityManager em;

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry metricRegistry;

    @Test
    @Transactional
    public void testMetrics() {
        assertEquals(0L, getCounterValueOrNull("hibernate.query.executions",
                new Tag("entityManagerFactory", PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)));
        assertEquals(0L, getCounterValueOrNull("hibernate.entities.inserts",
                new Tag("entityManagerFactory", PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)));
        assertEquals(0L, getCounterValueOrNull("hibernate.cache.query.requests",
                new Tag("entityManagerFactory", PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME),
                new Tag("result", "miss")));
        Arc.container().requestContext().activate();
        try {
            DummyEntity entity = new DummyEntity();
            entity.number = 12345L;
            em.persist(entity);
            em.flush();
            em.createQuery("from DummyEntity e").getResultList();
            assertEquals(1L, getCounterValueOrNull("hibernate.query.executions",
                    new Tag("entityManagerFactory", PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)));
            assertEquals(1L, getCounterValueOrNull("hibernate.entities.inserts",
                    new Tag("entityManagerFactory", PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)));
        } finally {
            Arc.container().requestContext().terminate();
        }
    }

    public Long getCounterValueOrNull(String metricName, Tag... tags) {
        Counter metric = metricRegistry.getCounters().get(new MetricID(metricName, tags));
        return metric != null ? metric.getCount() : null;
    }

}
