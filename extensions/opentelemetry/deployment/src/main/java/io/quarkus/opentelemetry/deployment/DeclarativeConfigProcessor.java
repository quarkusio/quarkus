package io.quarkus.opentelemetry.deployment;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;

import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.opentelemetry.runtime.DelayedAttributes;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.runtime.ApplicationConfig;
import io.smallrye.common.annotation.Identifier;

/**
 * Build steps and bean wiring for initializing the OpenTelemetry SDK from the declarative configuration model
 * ({@link QuarkusDeclarativeSdk}).
 */
@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class DeclarativeConfigProcessor {

    private static final String COMPONENT_PROVIDER_SERVICES = "META-INF/services/" + ComponentProvider.class.getName();
    private static final String DECLARATIVE_CONFIG_REFLECT_CONFIG = "META-INF/native-image/io.opentelemetry/"
            + "io.opentelemetry.sdk.autoconfigure.declarativeconfig/reflect-config.json";

    /**
     * Registers the {@link ComponentProvider} services for native images. {@code QuarkusDeclarativeSdk} resolves a
     * name through them only when no {@code Configurable*Provider} claims it. The upstream OTLP component providers
     * are left out and their service file removed: they need an HTTP sender the application has to add, and the
     * {@code otlp} exporter resolves through its {@code Configurable*Provider} as on the autoconfigure path.
     */
    @BuildStep
    void registerComponentProviders(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<RemovedResourceBuildItem> removedResources) throws IOException {
        List<String> componentProviders = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                COMPONENT_PROVIDER_SERVICES)
                .stream()
                .filter(provider -> !provider.startsWith("io.opentelemetry.exporter.otlp."))
                .toList();
        if (!componentProviders.isEmpty()) {
            services.produce(new ServiceProviderBuildItem(ComponentProvider.class.getName(), componentProviders));
        }
        removedResources.produce(new RemovedResourceBuildItem(
                ArtifactKey.fromString("io.opentelemetry:opentelemetry-exporter-otlp"),
                Set.of(COMPONENT_PROVIDER_SERVICES)));
    }

    /**
     * Removes the {@code reflect-config.json} of {@code opentelemetry-sdk-extension-declarative-config} from the
     * application. It registers the model classes and Jackson-backed classes such as {@code ModelMapper} for
     * reflection, and initializing {@code ModelMapper} fails without Jackson 2, which the runtime module excludes.
     * Quarkus builds the model with its setters and does not read it reflectively.
     */
    @BuildStep
    RemovedResourceBuildItem removeDeclarativeConfigReflection() {
        return new RemovedResourceBuildItem(
                ArtifactKey.fromString("io.opentelemetry:opentelemetry-sdk-extension-declarative-config"),
                Set.of(DECLARATIVE_CONFIG_REFLECT_CONFIG));
    }

    /**
     * Adds the injection points of the synthetic {@code OpenTelemetry} bean that {@code QuarkusDeclarativeSdk} looks
     * up. Each one must match a {@code SyntheticCreationalContext.getInjectedReference(...)} call there, type and
     * qualifiers included.
     * <p>
     * They are {@link Instance} injection points rather than {@code @All} lists: every injection point of a
     * synthetic bean is created before its creation function runs, so a list would instantiate the beans (starting
     * e.g. a user {@code SpanProcessor}'s worker thread) even on the disabled-SDK and legacy paths, which never use
     * them. An {@code Instance} creates beans only when iterated, and iterates in the same priority order.
     * </p>
     *
     * @param configurator the synthetic bean's configurator
     * @return {@code configurator}
     */
    static ExtendedBeanConfigurator addInjectionPoints(ExtendedBeanConfigurator configurator) {
        AnnotationInstance any = AnnotationInstance.builder(Any.class).build();
        return configurator
                .addInjectionPoint(ClassType.create(ApplicationConfig.class))
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(DelayedAttributes.class)),
                        any)
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(Resource.class)), any)
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(Sampler.class)), any)
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(IdGenerator.class)), any)
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(SpanProcessor.class)), any)
                .addInjectionPoint(
                        ParameterizedType.create(Instance.class, ClassType.create(TextMapPropagatorCustomizer.class)),
                        any)
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(Clock.class)))
                .addInjectionPoint(
                        ParameterizedType.create(Instance.class, ClassType.create(ScheduledExecutorService.class)))
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(MetricReader.class)),
                        AnnotationInstance.builder(Identifier.class)
                                .value(QuarkusDeclarativeSdk.DEV_UI_METRIC_READER)
                                .build())
                // default-qualified, matching the legacy SimpleLogRecordProcessorCustomizer's Instance resolution
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(LogRecordExporter.class)));
    }
}
