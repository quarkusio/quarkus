package io.quarkus.hibernate.orm.deployment;

import java.util.List;

import org.jboss.logmanager.Level;

import io.quarkus.runtime.logging.LogCleanupFilterElement;
import io.quarkus.runtime.logging.QuarkusBootstrapLogFilters;

/**
 * Declares the Hibernate ORM log cleanup filters that must be active before augmentation in
 * {@code @QuarkusTest} runs. These filters mirror the ones produced by
 * {@link HibernateLogFilterBuildStep}, ensuring they are applied even before the Quarkus recorder
 * has had a chance to register them through the normal build-step mechanism.
 *
 * @see QuarkusBootstrapLogFilters
 */
public class HibernateBootstrapLogFilters implements QuarkusBootstrapLogFilters {

    @Override
    public List<LogCleanupFilterElement> getLogCleanupFilters() {
        return List.of(
                new LogCleanupFilterElement("org.hibernate.orm.core", Level.DEBUG,
                        List.of("HHH000001")),
                new LogCleanupFilterElement("org.hibernate.orm.jpa", Level.DEBUG,
                        List.of("HHH008540")),
                new LogCleanupFilterElement("org.hibernate.statistics", Level.DEBUG,
                        List.of("HHH000400")),
                new LogCleanupFilterElement("org.hibernate.orm.beans", Level.DEBUG,
                        List.of("HHH10005002", "HHH10005004")),
                new LogCleanupFilterElement("org.hibernate.orm.incubating", Level.DEBUG,
                        List.of("HHH90006001")),
                new LogCleanupFilterElement("org.hibernate.orm.connections.pooling", Level.DEBUG,
                        List.of("HHH10001005")),
                new LogCleanupFilterElement("org.hibernate.orm.deprecation", Level.DEBUG,
                        List.of("HHH90000025")));
    }
}
