package io.quarkus.opentelemetry.deployment;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder.OPEN_TELEMETRY_DRIVER;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.opentelemetry.runtime.OpenTelemetryProducer;
import io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.tracing.cdi.WithSpanInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.InstrumentationRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class OpenTelemetryProcessor {

    private static final DotName LEGACY_WITH_SPAN = DotName.createSimple(
            io.opentelemetry.extension.annotations.WithSpan.class.getName());
    private static final DotName WITH_SPAN = DotName.createSimple(WithSpan.class.getName());
    private static final DotName SPAN_KIND = DotName.createSimple(SpanKind.class.getName());
    private static final DotName WITH_SPAN_INTERCEPTOR = DotName.createSimple(WithSpanInterceptor.class.getName());
    private static final DotName SPAN_ATTRIBUTE = DotName.createSimple(SpanAttribute.class.getName());

    @BuildStep
    AdditionalBeanBuildItem ensureProducerIsRetained() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(OpenTelemetryProducer.class)
                .build();
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> services) throws IOException {
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ConfigurableSpanExporterProvider.class.getName()));
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
                .produce(new ReflectiveClassBuildItem(true, true, QuarkusContextStorage.class));
    }

    @BuildStep
    void registerWithSpan(
            BuildProducer<InterceptorBindingRegistrarBuildItem> interceptorBindingRegistrar,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        interceptorBindingRegistrar.produce(new InterceptorBindingRegistrarBuildItem(
                new InterceptorBindingRegistrar() {
                    @Override
                    public List<InterceptorBinding> getAdditionalBindings() {
                        return List.of(InterceptorBinding.of(WithSpan.class, Set.of("value", "kind")));
                    }
                }));

        additionalBeans.produce(new AdditionalBeanBuildItem(WithSpanInterceptor.class));
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
            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                if (target.asClass().name().equals(WITH_SPAN_INTERCEPTOR)) {
                    transformationContext.transform().add(WITH_SPAN).done();
                }
            }
        }));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createOpenTelemetry(
            OpenTelemetryRecorder recorder,
            InstrumentationRecorder instrumentationRecorder,
            CoreVertxBuildItem vertx,
            LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdownContextBuildItem) {

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT || launchMode.getLaunchMode() == LaunchMode.TEST) {
            recorder.resetGlobalOpenTelemetryForDevMode();
        }

        RuntimeValue<OpenTelemetry> openTelemetry = recorder.createOpenTelemetry(shutdownContextBuildItem);

        recorder.eagerlyCreateContextStorage();
        recorder.storeVertxOnContextStorage(vertx.getVertx());

        instrumentationRecorder.setTracer(instrumentationRecorder.createTracers(openTelemetry));
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
                                                "Data source '%s' is using unsupported JDBC driver '%s', please active JDBC instrumentation with the 'quarkus.datasource.jdbc.telemetry=true' configuration property instead",
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
