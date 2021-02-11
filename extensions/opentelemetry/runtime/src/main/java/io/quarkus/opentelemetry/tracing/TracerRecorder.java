package io.quarkus.opentelemetry.tracing;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TracerRecorder {
    public RuntimeValue<SdkTracerProvider> createTracerProvider(String serviceName, String serviceVersion,
            ShutdownContext shutdownContext) {
        BeanManager beanManager = Arc.container().beanManager();
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder();

        // Define Service Resource
        builder.setResource(
                Resource.getDefault()
                        .merge(
                                Resource.create(
                                        Attributes.of(
                                                ResourceAttributes.SERVICE_NAME, serviceName,
                                                ResourceAttributes.SERVICE_VERSION, serviceVersion))));

        // Find all SpanExporter instances
        Instance<SpanExporter> allExporters = beanManager.createInstance()
                .select(SpanExporter.class, Any.Literal.INSTANCE);
        allExporters.forEach(spanExporter -> builder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter)));

        // Find all SpanProcessor instances
        Instance<SpanProcessor> allProcessors = beanManager.createInstance()
                .select(SpanProcessor.class, Any.Literal.INSTANCE);
        allProcessors.forEach(builder::addSpanProcessor);

        SdkTracerProvider tracerProvider = builder.build();

        // Register shutdown tasks
        shutdownContext.addShutdownTask(() -> {
            tracerProvider.forceFlush();
            tracerProvider.shutdown();
        });

        return new RuntimeValue<>(tracerProvider);
    }
}
