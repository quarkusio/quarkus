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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Smoke test for metrics exposed by the smallrye-metrics extension and computed from Hibernate statistics objects.
 */
public class HibernateMetricsTestCase {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addAsResource("application-metrics-enabled.properties", "application.properties")
            .addClasses(DummyEntity.class));

    @Entity(name = "DummyEntity")
    static class DummyEntity {

        @Id
        private Long number;

        public DummyEntity(Long number) {
            this.number = number;
        }

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
        assertEquals(0L, getCounterValueOrNull("hibernate-orm.queries.executed"));
        assertEquals(0L, getCounterValueOrNull("hibernate-orm.entities.inserted"));
        Arc.container().requestContext().activate();
        try {
            DummyEntity entity = new DummyEntity(12345L);
            em.persist(entity);
            em.flush();
            em.createQuery("from DummyEntity e").getResultList();
            assertEquals(1L, getCounterValueOrNull("hibernate-orm.queries.executed"));
            assertEquals(1L, getCounterValueOrNull("hibernate-orm.entities.inserted"));
        } finally {
            Arc.container().requestContext().terminate();
        }
    }

    public Long getCounterValueOrNull(String metricName, Tag... tags) {
        Counter metric = metricRegistry.getCounters().get(new MetricID(metricName, tags));
        return metric != null ? metric.getCount() : null;
    }

}
