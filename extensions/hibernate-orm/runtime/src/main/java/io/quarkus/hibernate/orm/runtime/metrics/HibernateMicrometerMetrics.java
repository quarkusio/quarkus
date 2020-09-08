package io.quarkus.hibernate.orm.runtime.metrics;

import org.hibernate.SessionFactory;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;
import io.micrometer.core.instrument.binder.jpa.HibernateQueryMetrics;

/**
 * Delay reference to micrometer dependencies
 */
public class HibernateMicrometerMetrics {
    /**
     * Register Micrometer meter binders
     *
     * @param puName persistence unit name (entity manager)
     * @param sessionFactory SessionFactory to instrument
     */
    static void registerMeterBinders(String puName, SessionFactory sessionFactory) {

        // Configure HibernateMetrics (micrometer adds tag for persistence unit)
        HibernateMetrics.monitor(
                Metrics.globalRegistry, sessionFactory,
                puName, Tags.empty());

        // Configure HibernateQueryMetrics (micrometer adds tag for persistence unit)
        HibernateQueryMetrics.monitor(
                Metrics.globalRegistry, sessionFactory,
                puName, Tags.empty());
    }
}
