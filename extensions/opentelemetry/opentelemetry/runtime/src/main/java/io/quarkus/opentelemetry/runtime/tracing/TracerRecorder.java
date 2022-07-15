package io.quarkus.opentelemetry.runtime.tracing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.arc.Arc;
import io.quarkus.opentelemetry.runtime.config.OtelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.OtelRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * All methods related with the Tracer Provider creation
 */
@Recorder
public class TracerRecorder {

    /* STATIC INIT */
    public RuntimeValue<Optional<IdGenerator>> createIdGenerator() {
        BeanManager beanManager = Arc.container().beanManager();

        Instance<IdGenerator> idGenerator = beanManager.createInstance()
                .select(IdGenerator.class, Any.Literal.INSTANCE);

        return new RuntimeValue<>(idGenerator.isResolvable() ? Optional.of(idGenerator.get()) : Optional.empty());
    }

    /* STATIC INIT */
    public RuntimeValue<Optional<Resource>> createResource(
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

        return new RuntimeValue<>(Optional.of(Resource.create(delayedAttributes)));
    }

    /* STATIC INIT */
    public RuntimeValue<Optional<LateBoundSampler>> createLateBoundSampler() {
        BeanManager beanManager = Arc.container().beanManager();

        LateBoundSampler lateBoundSampler = beanManager.createInstance()
                .select(LateBoundSampler.class, Any.Literal.INSTANCE).get();

        // Set LateBoundSampler
        return new RuntimeValue<>(Optional.of(lateBoundSampler));
    }

    /* STATIC INIT */
    public RuntimeValue<List<SpanExporter>> createSpanExporter() {
        BeanManager beanManager = Arc.container().beanManager();
        // Find all SpanExporter instances
        Instance<SpanExporter> allExporters = beanManager.createInstance() // FIXME not needed anymore... loaded by SPI
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

    /* RUNTIME INIT */
    public void setupResources(OtelRuntimeConfig config) {
        // Find all Resource instances
        Instance<Resource> allResources = CDI.current()
                .getBeanManager()
                .createInstance()
                .select(Resource.class, Any.Literal.INSTANCE);

        // Merge resource instances with env attributes
        Resource resource = allResources.stream()
                .reduce(Resource.empty(), Resource::merge)
                .merge(Optional.of(TracerUtil.mapResourceAttributes(config.resourceAttributes()))
                        .orElse(Resource.empty()));

        // Update Delayed attributes to contain new runtime attributes if necessary
        if (resource.getAttributes().size() > 0) {
            DelayedAttributes delayedAttributes = CDI.current()
                    .select(DelayedAttributes.class).get();

            delayedAttributes.setAttributesDelegate(
                    delayedAttributes.toBuilder()
                            .putAll(resource.getAttributes())
                            .build());
        }
    }

    /* RUNTIME INIT */
    public void setupSampler(
            OtelRuntimeConfig config,
            OtelBuildConfig buildConfig,
            List<String> dropNonApplicationUris,
            List<String> dropStaticResources) {

        LateBoundSampler lateBoundSampler = CDI.current().select(LateBoundSampler.class, Any.Literal.INSTANCE).get();
        Optional<Sampler> samplerBean = CDI.current()
                .select(Sampler.class, Any.Literal.INSTANCE)
                .stream()
                .filter(o -> !(o instanceof LateBoundSampler))
                .findFirst();
        if (samplerBean.isPresent()) {
            lateBoundSampler.setSamplerDelegate(samplerBean.get());
        } else {
            List<String> dropNames = new ArrayList<>();
            if (config.traces().extra().suppressNonApplicationUris()) {
                dropNames.addAll(dropNonApplicationUris);
            }
            if (!config.traces().extra().includeStaticResources()) {
                dropNames.addAll(dropStaticResources);
            }
            lateBoundSampler.setSamplerDelegate(TracerUtil.mapSampler(buildConfig.traces().sampler(), dropNames));
        }
    }
}
