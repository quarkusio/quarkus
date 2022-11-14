package io.quarkus.opentelemetry.deployment.tracing;

import java.util.Optional;

import io.opentelemetry.sdk.trace.IdGenerator;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class TracerIdGeneratorBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Optional<IdGenerator>> idGenerator;

    public TracerIdGeneratorBuildItem(RuntimeValue<Optional<IdGenerator>> idGenerator) {
        this.idGenerator = idGenerator;
    }

    public RuntimeValue<Optional<IdGenerator>> getIdGenerator() {
        return idGenerator;
    }
}
