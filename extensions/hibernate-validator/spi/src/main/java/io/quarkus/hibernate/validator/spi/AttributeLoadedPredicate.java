package io.quarkus.hibernate.validator.spi;

/**
 * Determines whether a given attribute of an entity is already loaded.
 * <p>
 * Used by the Hibernate Validator extension to build a {@code TraversableResolver} that skips
 * traversal of lazy attributes that are not loaded.
 */
public interface AttributeLoadedPredicate {

    /**
     * @param entity the entity instance, may be {@code null}
     * @param attributeName the name of the attribute
     * @return {@code true} if the attribute is loaded, {@code false} otherwise
     */
    boolean test(Object entity, String attributeName);
}
