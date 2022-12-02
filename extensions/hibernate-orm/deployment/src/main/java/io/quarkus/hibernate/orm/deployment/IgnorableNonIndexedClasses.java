package io.quarkus.hibernate.orm.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Indicates Hibernate feature related classes that don't need to be in the Jandex index because they are never used
 *
 * Various Spring Data JPA support classes are prime examples of such classes
 */
public final class IgnorableNonIndexedClasses extends MultiBuildItem {

    private final Set<String> classes;

    public IgnorableNonIndexedClasses(Set<String> classes) {
        this.classes = classes;
    }

    public Set<String> getClasses() {
        return classes;
    }
}
