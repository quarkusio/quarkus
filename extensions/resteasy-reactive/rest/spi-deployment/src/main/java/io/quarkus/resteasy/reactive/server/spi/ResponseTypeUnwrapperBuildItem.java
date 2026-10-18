package io.quarkus.resteasy.reactive.server.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers a parameterized wrapper type (e.g. {@code ResponseEntity<T>}) whose first type argument
 * should be treated as the actual response body type for reflection-free Jackson serializer generation.
 * <p>
 * When a REST endpoint returns one of these wrapper types, the serializer generator will extract the
 * type parameter and generate a serializer for the inner type instead of the wrapper itself.
 */
public final class ResponseTypeUnwrapperBuildItem extends MultiBuildItem {

    private final DotName wrapperType;

    public ResponseTypeUnwrapperBuildItem(DotName wrapperType) {
        this.wrapperType = wrapperType;
    }

    public DotName getWrapperType() {
        return wrapperType;
    }
}
