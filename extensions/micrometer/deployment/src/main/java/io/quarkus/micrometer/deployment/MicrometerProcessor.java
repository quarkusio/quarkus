package io.quarkus.micrometer.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.micrometer.deployment.export.PrometheusRegistryProcessor;
import io.quarkus.micrometer.deployment.export.RegistryBuildItem;
import io.quarkus.micrometer.runtime.ClockProvider;
import io.quarkus.micrometer.runtime.CompositeRegistryCreator;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;
import io.quarkus.micrometer.runtime.MeterFilterConstraints;
import io.quarkus.micrometer.runtime.MeterRegistryCustomizer;
import io.quarkus.micrometer.runtime.MeterRegistryCustomizerConstraint;
import io.quarkus.micrometer.runtime.MeterRegistryCustomizerConstraints;
import io.quarkus.micrometer.runtime.MeterTagsSupport;
import io.quarkus.micrometer.runtime.MicrometerCounted;
import io.quarkus.micrometer.runtime.MicrometerCountedInterceptor;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.MicrometerTimedInterceptor;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.export.exemplars.NoopOpenTelemetryExemplarContextUnwrapper;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

@BuildSteps(onlyIf = MicrometerProcessor.MicrometerEnabled.class)
public class MicrometerProcessor {
    private static final DotName METER_REGISTRY = DotName.createSimple(MeterRegistry.class.getName());
    private static final DotName METER_BINDER = DotName.createSimple(MeterBinder.class.getName());
    private static final DotName METER_FILTER = DotName.createSimple(MeterFilter.class.getName());
    private static final DotName METER_REGISTRY_CUSTOMIZER = DotName.createSimple(MeterRegistryCustomizer.class.getName());
    private static final DotName NAMING_CONVENTION = DotName.createSimple(NamingConvention.class.getName());

    private static final DotName COUNTED_ANNOTATION = DotName.createSimple(Counted.class.getName());
    private static final DotName COUNTED_BINDING = DotName.createSimple(MicrometerCounted.class.getName());
    private static final DotName COUNTED_INTERCEPTOR = DotName.createSimple(MicrometerCountedInterceptor.class.getName());
    private static final DotName TIMED_ANNOTATION = DotName.createSimple(Timed.class.getName());
    private static final DotName TIMED_INTERCEPTOR = DotName.createSimple(MicrometerTimedInterceptor.class.getName());
    private static final DotName METER_TAG_SUPPORT = DotName.createSimple(MeterTagsSupport.class.getName());

    public static class MicrometerEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return mConfig.enabled;
        }
    }

    MicrometerConfig mConfig;

    @BuildStep(onlyIfNot = PrometheusRegistryProcessor.PrometheusEnabled.class)
    MetricsCapabilityBuildItem metricsCapabilityBuildItem() {
        return new MetricsCapabilityBuildItem(MetricsFactory.MICROMETER::equals,
                null);
    }

    @BuildStep(onlyIfNot = PrometheusRegistryProcessor.PrometheusEnabled.class)
    void registerEmptyExamplarProvider(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(NoopOpenTelemetryExemplarContextUnwrapper.class)
                .setUnremovable()
                .build());
    }

    @BuildStep(onlyIf = { PrometheusRegistryProcessor.PrometheusEnabled.class })
    MetricsCapabilityBuildItem metricsCapabilityPrometheusBuildItem(
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        return new MetricsCapabilityBuildItem(MetricsFactory.MICROMETER::equals,
                nonApplicationRootPathBuildItem.resolvePath(mConfig.export.prometheus.path));
    }

    @BuildStep
    UnremovableBeanBuildItem registerAdditionalBeans(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<MicrometerRegistryProviderBuildItem> providerClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<InterceptorBindingRegistrarBuildItem> interceptorBindings) {

        // Create and keep some basic Providers
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(ClockProvider.class)
                .addBeanClass(CompositeRegistryCreator.class)
                .addBeanClass(MeterRegistryCustomizer.class)
                .build());

        // Add annotations and associated interceptors
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(MeterFilterConstraint.class)
                .addBeanClass(MeterFilterConstraints.class)
                .addBeanClass(MeterRegistryCustomizerConstraint.class)
                .addBeanClass(MeterRegistryCustomizerConstraints.class)
                .addBeanClass(TIMED_ANNOTATION.toString())
                .addBeanClass(TIMED_INTERCEPTOR.toString())
                .addBeanClass(COUNTED_ANNOTATION.toString())
                .addBeanClass(COUNTED_BINDING.toString())
                .addBeanClass(COUNTED_INTERCEPTOR.toString())
                .addBeanClass(METER_TAG_SUPPORT.toString())
                .build());

        // @Timed is registered as an additional interceptor binding
        interceptorBindings.produce(new InterceptorBindingRegistrarBuildItem(new InterceptorBindingRegistrar() {
            @Override
            public List<InterceptorBinding> getAdditionalBindings() {
                return List.of(InterceptorBinding.of(Timed.class, m -> true));
            }
        }));

        IndexView index = indexBuildItem.getIndex();

        reflectiveClasses.produce(createReflectiveBuildItem(COUNTED_ANNOTATION, index));
        reflectiveClasses.produce(createReflectiveBuildItem(TIMED_ANNOTATION, index));
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("org.HdrHistogram.Histogram",
                        "org.HdrHistogram.DoubleHistogram",
                        "org.HdrHistogram.ConcurrentHistogram")
                .build());

        return UnremovableBeanBuildItem.beanTypes(METER_REGISTRY, METER_BINDER, METER_FILTER, METER_REGISTRY_CUSTOMIZER,
                NAMING_CONVENTION);
    }

    @BuildStep
    AnnotationsTransformerBuildItem processAnnotatedMetrics(
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                // @Counted is only applicable to a method
                return kind == Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                final Collection<AnnotationInstance> annotations = ctx.getAnnotations();
                AnnotationInstance counted = Annotations.find(annotations, COUNTED_ANNOTATION);
                if (counted == null) {
                    return;
                }
                // Copy all the values so that the interceptor can use the binding annotation instead of java.lang.reflect.Method
                ctx.transform().add(COUNTED_BINDING, counted.values().toArray(new AnnotationValue[] {})).done();
            }
        });
    }

    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    @Record(ExecutionTime.STATIC_INIT)
    RootMeterRegistryBuildItem createRootRegistry(MicrometerRecorder recorder,
            MicrometerConfig config,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        RuntimeValue<MeterRegistry> registry = recorder.createRootRegistry(config,
                nonApplicationRootPathBuildItem.getNonApplicationRootPath(),
                nonApplicationRootPathBuildItem.getNormalizedHttpRootPath());
        return new RootMeterRegistryBuildItem(registry);
    }

    @BuildStep
    @Consume(RootMeterRegistryBuildItem.class)
    @Record(ExecutionTime.STATIC_INIT)
    void registerExtensionMetrics(MicrometerRecorder recorder,
            List<MetricsFactoryConsumerBuildItem> metricsFactoryConsumerBuildItems) {

        for (MetricsFactoryConsumerBuildItem item : metricsFactoryConsumerBuildItems) {
            if (item != null && item.executionTime() == ExecutionTime.STATIC_INIT) {
                recorder.registerMetrics(item.getConsumer());
            }
        }
    }

    @BuildStep
    @Consume(RootMeterRegistryBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureRegistry(MicrometerRecorder recorder,
            MicrometerConfig config,
            List<MicrometerRegistryProviderBuildItem> providerClassItems,
            List<MetricsFactoryConsumerBuildItem> metricsFactoryConsumerBuildItems,
            ShutdownContextBuildItem shutdownContextBuildItem,
            BuildProducer<SystemPropertyBuildItem> systemProperty) {

        // Avoid users from receiving:
        // [io.mic.cor.ins.com.CompositeMeterRegistry] (main) A MeterFilter is being configured after a Meter has been
        // registered to this registry...
        // It's unavoidable because of how Quarkus startup works and users cannot do anything about it.
        // see: https://github.com/micrometer-metrics/micrometer/issues/4920#issuecomment-2298348202
        systemProperty.produce(
                new SystemPropertyBuildItem(
                        "quarkus.log.category.\"io.micrometer.core.instrument.composite.CompositeMeterRegistry\".level",
                        "ERROR"));

        Set<Class<? extends MeterRegistry>> typeClasses = new HashSet<>();
        for (MicrometerRegistryProviderBuildItem item : providerClassItems) {
            typeClasses.add(item.getRegistryClass());
        }

        // Runtime config at play here: host+port, API keys, etc.
        recorder.configureRegistries(config, typeClasses, shutdownContextBuildItem);

        for (MetricsFactoryConsumerBuildItem item : metricsFactoryConsumerBuildItems) {
            if (item != null && item.executionTime() == ExecutionTime.RUNTIME_INIT) {
                recorder.registerMetrics(item.getConsumer());
            }
        }
    }

    ReflectiveClassBuildItem createReflectiveBuildItem(DotName sourceAnnotation, IndexView index) {
        Set<String> classes = new HashSet<>();

        for (AnnotationInstance annotation : index.getAnnotations(sourceAnnotation)) {
            AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case METHOD:
                    MethodInfo method = target.asMethod();
                    classes.add(method.declaringClass().name().toString());
                    break;
                case TYPE:
                    classes.add(target.asClass().name().toString());
                    break;
                default:
                    break;
            }
        }

        return ReflectiveClassBuildItem.builder(classes.toArray(new String[0])).reason(getClass().getName()).build();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem createCard(List<RegistryBuildItem> registries) {
        var card = new CardPageBuildItem();

        registries.stream().filter(r -> "JSON".equalsIgnoreCase(r.name())).findFirst().ifPresent(r -> {
            card.addPage(Page.externalPageBuilder("JSON")
                    .icon("font-awesome-solid:chart-line")
                    .url(r.path())
                    .isJsonContent());
        });

        registries.stream().filter(r -> "Prometheus".equalsIgnoreCase(r.name())).findFirst().ifPresent(r -> {
            card.addPage(Page.externalPageBuilder("Prometheus")
                    .icon("font-awesome-solid:chart-line")
                    .url(r.path())
                    .isJsonContent());
            card.addPage(Page.externalPageBuilder("Prometheus (raw output)")
                    .doNotEmbed()
                    .icon("font-awesome-solid:up-right-from-square")
                    .url(r.path())
                    .mimeType("text/plain"));
        });

        return card;
    }

}
