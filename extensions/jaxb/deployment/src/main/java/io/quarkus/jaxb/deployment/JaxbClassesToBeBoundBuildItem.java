package io.quarkus.jaxb.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * List of class names to be bound in the JAXB context. Note that some of the class names can be removed via
 * {@code quarkus.jaxb.exclude-classes}.
 *
 * @see FilteredJaxbClassesToBeBoundBuildItem
 */
public final class JaxbClassesToBeBoundBuildItem extends MultiBuildItem {

    private final List<String> classes;

    public JaxbClassesToBeBoundBuildItem(List<String> classes) {
        this.classes = Objects.requireNonNull(Collections.unmodifiableList(new ArrayList<>(classes)));
    }

    public List<String> getClasses() {
        return classes;
    }
}
