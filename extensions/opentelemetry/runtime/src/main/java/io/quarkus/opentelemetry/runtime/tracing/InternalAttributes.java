package io.quarkus.opentelemetry.runtime.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

/**
 * Contains utilities for working with internal Quarkus attributes
 */
public final class InternalAttributes {

    public static final AttributeKey<String> TRACELESS = AttributeKey.stringKey("TRACELESS");

    public static boolean containsTraceless(Attributes attributes) {
        return Boolean.parseBoolean(attributes.get(TRACELESS));
    }

    private InternalAttributes() {
    }
}
