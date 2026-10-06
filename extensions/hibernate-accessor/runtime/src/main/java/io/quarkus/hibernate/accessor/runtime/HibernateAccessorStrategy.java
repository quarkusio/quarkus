package io.quarkus.hibernate.accessor.runtime;

/**
 * Selects how the Hibernate accessor factory accesses registered members.
 */
public enum HibernateAccessorStrategy {
    /**
     * Uses generated accessors and rejects members that were not registered at build time.
     */
    REFLECTION_FREE,
    /**
     * Uses generated accessors where available and falls back to reflection for other members.
     */
    REFLECTION_FREE_WITH_FALLBACK,
    /**
     * Uses reflection for all members and disables accessor bytecode generation.
     */
    REFLECTION
}
