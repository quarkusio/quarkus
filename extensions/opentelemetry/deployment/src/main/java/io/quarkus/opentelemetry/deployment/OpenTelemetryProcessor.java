package io.quarkus.opentelemetry.deployment;

import static io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem.SPI_ROOT;
import static io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder.OPEN_TELEMETRY_DRIVER;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.exporter.otlp.internal.OtlpSpanExporterProvider;
import io.opentelemetry.instrumentation.annotations.AddingSpanAttributes;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.OpenTelemetryInitBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.opentelemetry.OpenTelemetryDestroyer;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.config.build.ExporterType;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.cdi.AddingSpanAttributesInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.cdi.WithSpanInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.InstrumentationRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class OpenTelemetryProcessor {

    private static final DotName LEGACY_WITH_SPAN = DotName.createSimple(
            io.opentelemetry.extension.annotations.WithSpan.class.getName());
    private static final DotName WITH_SPAN = DotName.createSimple(WithSpan.class.getName());
    private static final DotName ADD_SPAN_ATTRIBUTES = DotName.createSimple(AddingSpanAttributes.class.getName());
    private static final Predicate<AnnotationInstance> isAddSpanAttribute = new Predicate<>() {
        @Override
        public boolean test(AnnotationInstance annotationInstance) {
            return annotationInstance.name().equals(ADD_SPAN_ATTRIBUTES);
        }
    };
    private static final DotName SPAN_KIND = DotName.createSimple(SpanKind.class.getName());
    private static final DotName WITH_SPAN_INTERCEPTOR = DotName.createSimple(WithSpanInterceptor.class.getName());
    private static final DotName ADD_SPAN_ATTRIBUTES_INTERCEPTOR = DotName
            .createSimple(AddingSpanAttributesInterceptor.class.getName());

    @BuildStep
    AdditionalBeanBuildItem ensureProducerIsRetained() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.ResourceCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.SamplerCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TracerProviderCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TextMapPropagatorCustomizers.class)
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem openTelemetryBean(OpenTelemetryRecorder recorder, OTelRuntimeConfig oTelRuntimeConfig) {
        return SyntheticBeanBuildItem.configure(OpenTelemetry.class)
                .defaultBean()
                .setRuntimeInit()
                .unremovable()
                .scope(Singleton.class)
                .addInjectionPoint(
                        ParameterizedType.create(
                                DotName.createSimple(Instance.class),
                                new Type[] { ClassType.create(
                                        DotName.createSimple(
                                                AutoConfiguredOpenTelemetrySdkBuilderCustomizer.class.getName())) },
                                null))
                .createWith(recorder.opentelemetryBean(oTelRuntimeConfig))
                .destroyer(OpenTelemetryDestroyer.class)
                .done();
    }

    @BuildStep
    void handleServices(OTelBuildConfig config,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<RemovedResourceBuildItem> removedResources,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitialized) throws IOException {

        List<String> spanExporterProviders = ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                SPI_ROOT + ConfigurableSpanExporterProvider.class.getName())
                .stream()
                .filter(p -> !OtlpSpanExporterProvider.class.getName().equals(p)).collect(toList()); // filter out OtlpSpanExporterProvider since it depends on OkHttp
        if (!spanExporterProviders.isEmpty()) {
            services.produce(
                    new ServiceProviderBuildItem(ConfigurableSpanExporterProvider.class.getName(), spanExporterProviders));
        }
        // remove the service file that contains OtlpSpanExporterProvider
        if (config.traces().exporter().stream().noneMatch(ExporterType.Constants.OTLP_VALUE::equals)) {
            removedResources.produce(new RemovedResourceBuildItem(
                    ArtifactKey.fromString("io.opentelemetry:opentelemetry-exporter-otlp"),
                    Set.of("META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")));
        }

        runtimeReinitialized.produce(
                new RuntimeReinitializedClassBuildItem("io.opentelemetry.sdk.autoconfigure.TracerProviderConfiguration"));

        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ConfigurableSamplerProvider.class.getName()));

        // The following are added but not officially supported, yet.
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                AutoConfigurationCustomizerProvider.class.getName()));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ResourceProvider.class.getName()));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ConfigurablePropagatorProvider.class.getName()));
    }

    @BuildStep
    void registerOpenTelemetryContextStorage(
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.opentelemetry.context.ContextStorageProvider"));
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(QuarkusContextStorage.class).methods().fields().build());
    }

    @BuildStep
    void registerWithSpan(
            BuildProducer<InterceptorBindingRegistrarBuildItem> interceptorBindingRegistrar,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        interceptorBindingRegistrar.produce(new InterceptorBindingRegistrarBuildItem(
                new InterceptorBindingRegistrar() {
                    @Override
                    public List<InterceptorBinding> getAdditionalBindings() {
                        return List.of(
                                InterceptorBinding.of(WithSpan.class, Set.of("value", "kind")),
                                InterceptorBinding.of(AddingSpanAttributes.class, Set.of("value")));
                    }
                }));

        additionalBeans.produce(new AdditionalBeanBuildItem(
                WithSpanInterceptor.class,
                AddingSpanAttributesInterceptor.class));
    }

    @BuildStep
    void transformWithSpan(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {

        // Transform deprecated annotation into new one
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext context) {
                final AnnotationTarget target = context.getTarget();

                List<AnnotationInstance> legacyWithSpans = context.getAnnotations().stream()
                        .filter(annotationInstance -> annotationInstance.name().equals(LEGACY_WITH_SPAN))
                        .collect(toList());

                for (AnnotationInstance legacyAnnotation : legacyWithSpans) {
                    AnnotationValue value = Optional.ofNullable(legacyAnnotation.value("value"))
                            .orElse(AnnotationValue.createStringValue("value", ""));
                    AnnotationValue kind = Optional.ofNullable(legacyAnnotation.value("kind"))
                            .orElse(AnnotationValue.createEnumValue("kind", SPAN_KIND, SpanKind.INTERNAL.name()));
                    AnnotationInstance annotation = AnnotationInstance.create(
                            WITH_SPAN,
                            target,
                            List.of(value, kind));
                    context.transform().add(annotation).done();
                }
            }
        }));

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(transformationContext -> {
            AnnotationTarget target = transformationContext.getTarget();
            Transformation transform = transformationContext.transform();
            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                if (target.asClass().name().equals(WITH_SPAN_INTERCEPTOR)) {
                    transform.add(WITH_SPAN);
                } else if (target.asClass().name().equals(ADD_SPAN_ATTRIBUTES_INTERCEPTOR)) {
                    transform.add(ADD_SPAN_ATTRIBUTES);
                }
            } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo methodInfo = target.asMethod();
                // WITH_SPAN_INTERCEPTOR and ADD_SPAN_ATTRIBUTES must not be applied at the same time and the first has priority.
                if (methodInfo.hasAnnotation(WITH_SPAN) && methodInfo.hasAnnotation(ADD_SPAN_ATTRIBUTES)) {
                    transform.remove(isAddSpanAttribute);
                }
            }
            transform.done();
        }));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(OpenTelemetryInitBuildItem.class)
    void createOpenTelemetry(
            OpenTelemetryRecorder recorder,
            CoreVertxBuildItem vertx,
            LaunchModeBuildItem launchMode) {

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT || launchMode.getLaunchMode() == LaunchMode.TEST) {
            recorder.resetGlobalOpenTelemetryForDevMode();
        }

        recorder.eagerlyCreateContextStorage();
        recorder.storeVertxOnContextStorage(vertx.getVertx());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupVertx(InstrumentationRecorder recorder, BeanContainerBuildItem beanContainerBuildItem,
            Capabilities capabilities) {
        boolean sqlClientAvailable = capabilities.isPresent(Capability.REACTIVE_DB2_CLIENT)
                || capabilities.isPresent(Capability.REACTIVE_MSSQL_CLIENT)
                || capabilities.isPresent(Capability.REACTIVE_MYSQL_CLIENT)
                || capabilities.isPresent(Capability.REACTIVE_ORACLE_CLIENT)
                || capabilities.isPresent(Capability.REACTIVE_PG_CLIENT);
        recorder.setupVertxTracer(beanContainerBuildItem.getValue(), sqlClientAvailable);
    }

    @BuildStep
    void validateDataSourcesWithEnabledTelemetry(List<JdbcDataSourceBuildItem> jdbcDataSources,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        for (JdbcDataSourceBuildItem dataSource : jdbcDataSources) {
            final String dataSourceName = dataSource.getName();

            // verify that no datasource is using OpenTelemetryDriver as that is not supported anymore
            if (dataSourceUsesOTelJdbcDriver(dataSourceName)) {
                validationErrors.produce(
                        new ValidationErrorBuildItem(
                                new ConfigurationException(
                                        String.format(
                                                "Data source '%s' is using unsupported JDBC driver '%s', please activate JDBC instrumentation by setting the 'quarkus.datasource.jdbc.telemetry' configuration property to 'true' instead",
                                                dataSourceName, OPEN_TELEMETRY_DRIVER))));
            }
        }
    }

    private static boolean dataSourceUsesOTelJdbcDriver(String dataSourceName) {
        List<String> driverPropertyKeys = DataSourceUtil.dataSourcePropertyKeys(dataSourceName, "jdbc.driver");
        for (String driverPropertyKey : driverPropertyKeys) {
            ConfigValue explicitlyConfiguredDriverValue = ConfigProvider.getConfig().getConfigValue(driverPropertyKey);
            if (explicitlyConfiguredDriverValue.getValue() != null) {
                return explicitlyConfiguredDriverValue.getValue().equals(OPEN_TELEMETRY_DRIVER);
            }
        }
        return false;
    }

}
