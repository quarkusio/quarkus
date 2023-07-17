package io.quarkus.opentelemetry.runtime.tracing;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TracerRecorder {

    public static final Set<String> dropNonApplicationUriTargets = new HashSet<>();
    public static final Set<String> dropStaticResourceTargets = new HashSet<>();

    /* STATIC INIT */
    public RuntimeValue<Optional<IdGenerator>> createIdGenerator() {
        BeanManager beanManager = Arc.container().beanManager();

        Instance<IdGenerator> idGenerator = beanManager.createInstance()
                .select(IdGenerator.class, Any.Literal.INSTANCE);

        return new RuntimeValue<>(idGenerator.isResolvable() ? Optional.of(idGenerator.get()) : Optional.empty());
    }

    /* STATIC INIT */
    public RuntimeValue<Resource> createResource(
            String quarkusVersion,
            String serviceName,
            String serviceVersion) {
        BeanManager beanManager = Arc.container().beanManager();

        DelayedAttributes delayedAttributes = beanManager.createInstance()
                .select(DelayedAttributes.class, Any.Literal.INSTANCE).get();

        delayedAttributes.setAttributesDelegate(Resource.getDefault()
                .merge(Resource.create(
                        Attributes.of(
                                ResourceAttributes.SERVICE_NAME, serviceName,
                                ResourceAttributes.SERVICE_VERSION, serviceVersion,
                                ResourceAttributes.WEBENGINE_NAME, "Quarkus",
                                ResourceAttributes.WEBENGINE_VERSION, quarkusVersion)))
                .getAttributes());

        return new RuntimeValue<>(Resource.create(delayedAttributes));
    }

    /* STATIC INIT */
    public RuntimeValue<List<SpanExporter>> createSpanExporter() {
        BeanManager beanManager = Arc.container().beanManager();
        // Find all SpanExporter instances
        Instance<SpanExporter> allExporters = beanManager.createInstance()
                .select(SpanExporter.class, Any.Literal.INSTANCE);

        return new RuntimeValue<>(allExporters.stream().collect(Collectors.toList()));
    }

    /* STATIC INIT */
    public RuntimeValue<List<SpanProcessor>> createSpanProcessors() {
        BeanManager beanManager = Arc.container().beanManager();

        // Find all SpanProcessor instances
        Instance<SpanProcessor> allProcessors = beanManager.createInstance()
                .select(SpanProcessor.class, Any.Literal.INSTANCE);

        return new RuntimeValue<>(allProcessors.stream().collect(Collectors.toList()));

    }

    /* STATIC INIT */
    public void setupSampler(
            List<String> dropNonApplicationUris,
            List<String> dropStaticResources) {
        dropNonApplicationUriTargets.addAll(dropNonApplicationUris);
        dropStaticResourceTargets.addAll(dropStaticResources);
    }
}
