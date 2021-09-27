package io.quarkus.arc.deployment;

import java.util.function.Predicate;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.ApplicationArchive;

/**
 * 
 * By default, only explict/implicit bean archives (as defined by the spec) are considered during the bean discovery. However,
 * extensions can register a logic to identify additional bean archives.
 */
public final class BeanArchivePredicateBuildItem extends MultiBuildItem {

    private final Predicate<ApplicationArchive> predicate;

    public BeanArchivePredicateBuildItem(Predicate<ApplicationArchive> predicate) {
        this.predicate = predicate;
    }

    public Predicate<ApplicationArchive> getPredicate() {
        return predicate;
    }

}
