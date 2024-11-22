package io.quarkus.resteasy.reactive.spi;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that provides a {@link Predicate} to detect and validate classes defining REST endpoints.
 * <p>
 * This can include resources in RESTEasy or controllers in the Spring ecosystem.
 * It acts as a Service Provider Interface (SPI) to allow customization of the validation logic for endpoint detection,
 * enabling integration with various frameworks or specific application needs.
 * </p>
 *
 * <p>
 * The {@link Predicate} evaluates {@link ClassInfo} instances to determine whether a class defines a REST endpoint
 * according to the provided logic.
 * </p>
 */
public final class EndpointValidationPredicatesBuildItem extends MultiBuildItem {

    private final Predicate<ClassInfo> predicate;

    public EndpointValidationPredicatesBuildItem(Predicate<ClassInfo> predicate) {
        this.predicate = predicate;
    }

    public Predicate<ClassInfo> getPredicate() {
        return predicate;
    }
}
