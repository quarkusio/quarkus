package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.List;
import java.util.Set;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeConfiguration;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;

/**
 * Resolves the {@link Sampler} declarative-config component named
 * {@link QuarkusOTelModelBuilder#QUARKUS_DROP_TARGETS}: wraps a delegate sampler so that spans for a configured set
 * of URI prefixes are never sampled, mirroring {@code io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler}
 * usage in {@code AutoConfiguredOpenTelemetrySdkBuilderCustomizer.SamplerCustomizer} for the legacy SDK
 * initialization path.
 * <p>
 * {@link QuarkusDeclarativeConfiguration} resolves this node inline without going through this provider; the
 * provider exists so the node also resolves under a standard declarative-config implementation (upstream's
 * {@code DeclarativeConfiguration.create()}, once it is usable - see the pom's dependency comment).
 * </p>
 */
public class QuarkusDropTargetsSamplerProvider implements ComponentProvider {

    @Override
    public Class<?> getType() {
        return Sampler.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        DeclarativeConfigProperties delegateNode = config.getStructured("delegate");
        Sampler delegate = delegateNode == null ? Sampler.parentBased(Sampler.alwaysOn())
                : QuarkusDeclarativeConfiguration.createSampler(delegateNode);
        // getScalarList returns null for an absent key; QuarkusOTelModelBuilder always sets it, but a
        // hand-written node reaching this provider through another declarative-config stack may not.
        List<String> dropTargets = config.getScalarList("drop_targets", String.class);
        return new DropTargetsSampler(delegate, dropTargets == null ? Set.of() : Set.copyOf(dropTargets));
    }
}
