package io.quarkus.jaxb.deployment;

import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * List of classes to be bound in the JAXB context.
 */
public final class JaxbClassesToBeBoundBuildItem extends MultiBuildItem {

    private final List<String> classes;

    public JaxbClassesToBeBoundBuildItem(List<String> classes) {
        this.classes = Objects.requireNonNull(classes);
    }

    public List<String> getClasses() {
        return classes;
    }
}
