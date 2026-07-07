package io.quarkus.gradle.application.internal.config;

import java.util.Objects;

public record EffectiveConfigDiagnostic(
        String key,
        String value,
        String source,
        int sourceOrdinal,
        boolean defaultValue) {

    public EffectiveConfigDiagnostic {
        key = Objects.requireNonNull(key, "key");
        value = Objects.requireNonNull(value, "value");
        source = Objects.requireNonNull(source, "source");
    }
}
