package io.quarkus.jaxb.deployment;

import java.util.function.Predicate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Selects class names to ignore when registering for reflection and binding in the default JAXB context even though
 * annotated with some of the JAXB root annotations.
 */
public final class IgnoreJaxbAnnotatedClassesBuildItem extends MultiBuildItem {

    private final Predicate<String> predicate;

    /**
     * @param predicate whose {@link Predicate#test(Object)} method returns {@code true} for class names which should
     *        be ignored when registering for reflection and binding in the default JAXB context
     */
    public IgnoreJaxbAnnotatedClassesBuildItem(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    public Predicate<String> getPredicate() {
        return predicate;
    }

}
