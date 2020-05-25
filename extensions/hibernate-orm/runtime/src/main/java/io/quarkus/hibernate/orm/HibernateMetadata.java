package io.quarkus.hibernate.orm;

import java.util.Optional;

/**
 * CDI bean that contains useful Hibernate metadata
 *
 * The bean will only be present if the following conditions apply
 *
 * <p>
 * <ul>
 * <li>There is at least one entity
 * <li>There exists at least one CDI injection point for the class
 * </ul>
 * <p>
 *
 * Warning: this API is experimental and subject to change as the Quarkus / Hibernate integration evolves
 */
public interface HibernateMetadata {

    String DEFAULT_PERSISTENCE_UNIT_NAME = "default";

    /**
     * Returns {@link PersistenceUnitMetadata} for a given named persistence unit
     */
    Optional<PersistenceUnitMetadata> getPersistenceUnitMetadata(String name);

    /**
     * Returns {@link PersistenceUnitMetadata} for the default persistence unit
     */
    default Optional<PersistenceUnitMetadata> getDefaultPersistenceUnitMetadata() {
        return getPersistenceUnitMetadata(DEFAULT_PERSISTENCE_UNIT_NAME);
    }
}
