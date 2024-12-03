package io.quarkus.resteasy.reactive.common.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class EndpointValidationPredicatesBuildItem extends MultiBuildItem {

    private final Predicate<ClassInfo> predicate;

    public EndpointValidationPredicatesBuildItem(Predicate<ClassInfo> predicate) {
        this.predicate = predicate;
    }

    public Predicate<ClassInfo> getPredicate() {
        return predicate;
    }
}
