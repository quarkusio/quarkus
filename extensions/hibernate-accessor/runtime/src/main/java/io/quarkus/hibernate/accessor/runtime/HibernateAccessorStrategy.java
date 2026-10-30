package io.quarkus.hibernate.accessor.runtime;

/**
 * Selects how the Hibernate accessor factory accesses members.
 */
public enum HibernateAccessorStrategy {
    /**
     * Uses generated accessors and rejects members that were not registered at build time.
     */
    GENERATED,
    /**
     * Uses generated accessors where available and falls back to reflection for other members.
     */
    GENERATED_WITH_FALLBACK,
    /**
     * Uses reflection for all members and disables accessor bytecode generation.
     */
    REFLECTION
}
