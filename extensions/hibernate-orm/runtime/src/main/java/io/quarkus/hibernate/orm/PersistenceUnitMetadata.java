package io.quarkus.hibernate.orm;

import java.util.Set;

/**
 * Class that contains metadata about a persistent unit.
 *
 * Warning: this API is experimental and subject to change as the Quarkus / Hibernate integration evolves
 *
 * @see HibernateMetadata
 */
public interface PersistenceUnitMetadata {

    Set<String> getEntityClassNames();

    /**
     * Convenience: load classes provided by {@link #getEntityClassNames()} using
     * {@code Thread.currentThread().getContextClassLoader()}.
     */
    Set<Class<?>> resolveEntityClasses();

}
