package io.quarkus.deployment.dev;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows disabling of instrumentation based reload if the changed class matches certain criteria
 */
public final class DisableInstrumentationForClassPredicateBuildItem extends MultiBuildItem
        implements Supplier<Predicate<ClassInfo>> {

    private final Predicate<ClassInfo> predicate;

    public DisableInstrumentationForClassPredicateBuildItem(Predicate<ClassInfo> predicate) {
        this.predicate = predicate;
    }

    public Predicate<ClassInfo> getPredicate() {
        return predicate;
    }

    @Override
    public Predicate<ClassInfo> get() {
        return getPredicate();
    }
}
