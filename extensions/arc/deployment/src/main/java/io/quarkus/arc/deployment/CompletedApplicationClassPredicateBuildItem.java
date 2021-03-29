package io.quarkus.arc.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This build item hold the "final" predicate that is used to distinguish application classes from framework/library classes.
 */
public final class CompletedApplicationClassPredicateBuildItem extends SimpleBuildItem implements Predicate<DotName> {

    private final Predicate<DotName> applicationClassPredicate;

    CompletedApplicationClassPredicateBuildItem(Predicate<DotName> applicationClassPredicate) {
        this.applicationClassPredicate = applicationClassPredicate;
    }

    public Predicate<DotName> getApplicationClassPredicate() {
        return applicationClassPredicate;
    }

    @Override
    public boolean test(DotName name) {
        return applicationClassPredicate.test(name);
    }

}
