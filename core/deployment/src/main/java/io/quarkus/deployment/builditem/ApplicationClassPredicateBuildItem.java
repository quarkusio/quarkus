package io.quarkus.deployment.builditem;

import java.util.function.Predicate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to identify wiring classes generated for classes from additional hot deployment paths.
 */
public final class ApplicationClassPredicateBuildItem extends MultiBuildItem {

    private final Predicate<String> predicate;

    public ApplicationClassPredicateBuildItem(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    public boolean test(String name) {
        return predicate.test(name);
    }

}
