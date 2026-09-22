package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Declares classes that are referenced by a Hibernate-related feature but do not need to appear
 * in the Jandex index because they are never used as JPA entities or mapped types.
 * <p>
 * Produced by extensions that pull in classes Hibernate would otherwise warn about as unindexed
 * (e.g. Spring Data JPA contributing Spring-specific repository base types).
 * <p>
 * Consumed during JPA model discovery: the listed classes are excluded from the
 * "not in Jandex index" validation, avoiding spurious warnings at build time.
 */
public final class IgnorableNonIndexedClasses extends MultiBuildItem {

    private final Set<String> classes;

    /**
     * @param classes fully-qualified class names to exclude from the "not in Jandex index" validation
     */
    public IgnorableNonIndexedClasses(Set<String> classes) {
        this.classes = classes;
    }

    public Set<String> getClasses() {
        return classes;
    }
}
