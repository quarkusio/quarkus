package io.quarkus.deployment.dev;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.jandex.Index;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows disabling of instrumentation based reload if the index of changed classes matches certain criteria
 */
public final class DisableInstrumentationForIndexPredicateBuildItem extends MultiBuildItem
        implements Supplier<Predicate<Index>> {

    private final Predicate<Index> predicate;

    public DisableInstrumentationForIndexPredicateBuildItem(Predicate<Index> predicate) {
        this.predicate = predicate;
    }

    public Predicate<Index> getPredicate() {
        return predicate;
    }

    @Override
    public Predicate<Index> get() {
        return getPredicate();
    }
}
