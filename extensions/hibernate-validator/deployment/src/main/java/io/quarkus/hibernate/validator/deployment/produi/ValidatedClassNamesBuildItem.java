package io.quarkus.hibernate.validator.deployment.produi;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the names of the classes that carry Bean Validation constraints, as
 * discovered at build time. Consumed by the Prod UI processor to seed the
 * runtime service so it can introspect their constraint metadata.
 */
public final class ValidatedClassNamesBuildItem extends SimpleBuildItem {

    private final Set<String> classNames;

    public ValidatedClassNamesBuildItem(Set<String> classNames) {
        this.classNames = classNames;
    }

    public Set<String> getClassNames() {
        return classNames;
    }
}
