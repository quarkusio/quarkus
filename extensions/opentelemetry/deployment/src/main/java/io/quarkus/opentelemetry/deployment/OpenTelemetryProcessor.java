package io.quarkus.opentelemetry.deployment;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isClassPresentAtRuntime;
import static io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem.SPI_ROOT;
import static io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder.OPEN_TELEMETRY_DRIVER;
import static io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider.CONTAINER_NAME_ENV;
import static io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider.DEPLOYMENT_NAME_ENV;
import static io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider.NAMESPACE_ENV;
import static io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider.NODE_NAME_ENV;
import static io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider.POD_NAME_ENV;
import static io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider.POD_UID_ENV;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigValue;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.internal.OtlpLogRecordExporterProvider;
import io.opentelemetry.exporter.otlp.internal.OtlpMetricExporterProvider;
import io.opentelemetry.exporter.otlp.internal.OtlpSpanExporterProvider;
import io.opentelemetry.instrumentation.annotations.AddingSpanAttributes;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.builder.Version;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.OpenTelemetrySdkBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.opentelemetry.OpenTelemetryDestroyer;
import io.quarkus.opentelemetry.deployment.metric.MetricsEnabled;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.opentelemetry.runtime.DelayedAttributes;
import io.quarkus.opentelemetry.runtime.InternalSdkBuilderCustomizer;
import io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.config.build.ExporterType;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.opentelemetry.runtime.tracing.cdi.AddingSpanAttributesInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.cdi.WithSpanInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.InstrumentationRecorder;
import io.quarkus.rest.client.reactive.spi.RegisteredRestClientBuildItem;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.smallrye.config.Config;

@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class OpenTelemetryProcessor {

    private static final Logger LOG = Logger.getLogger(OpenTelemetryProcessor.class);
    private static final DotName WITH_SPAN = DotName.createSimple(WithSpan.class.getName());
    private static final DotName ADD_SPAN_ATTRIBUTES = DotName.createSimple(AddingSpanAttributes.class.getName());
    private static final Predicate<AnnotationInstance> isAddSpanAttribute = new Predicate<>() {
        @Override
        public boolean test(AnnotationInstance annotationInstance) {
            return annotationInstance.name().equals(ADD_SPAN_ATTRIBUTES);
        }
    };
    private static final DotName WITH_SPAN_INTERCEPTOR = DotName.createSimple(WithSpanInterceptor.class.getName());
    // the annotation is package-private so that applications cannot use it, hence the name is spelled out
    private static final DotName SPAN_METHOD = DotName.createSimple("io.quarkus.opentelemetry.runtime.tracing.cdi.SpanMethod");
    private static final DotName ADD_SPAN_ATTRIBUTES_INTERCEPTOR = DotName
            .createSimple(AddingSpanAttributesInterceptor.class.getName());

    @BuildStep
    SystemPropertyBuildItem setSemconvStabilityOptIn() {
        // See: /io/opentelemetry/instrumentation/api/internal/SemconvStability.java:45
        return new SystemPropertyBuildItem("otel.semconv-stability.opt-in", "rpc");
    }

    @BuildStep(onlyIfNot = MetricsEnabled.class)
    void registerForReflection(BuildProducer<ReflectiveMethodBuildItem> reflectiveItem) {
        if (isClassPresentAtRuntime(
                "io.opentelemetry.exporter.logging.LoggingMetricExporter")) {
            reflectiveItem.produce(new ReflectiveMethodBuildItem(
                    "Used by OpenTelemetry Export Logging",
                    "io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil",
                    "addMeterConfiguratorCondition"));
        }
    }

    @SuppressWarnings("removal")
    @BuildStep
    AdditionalBeanBuildItem ensureProducerIsRetained() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.SimpleLogRecordProcessorCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.ResourceCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.SamplerCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.SpanProcessorCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TracerProviderCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.MetricProviderCustomizer.class,
                        AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TextMapPropagatorCustomizers.class)
                .build();
    }

    // Signal independent resource attributes
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem setupDelayedAttribute(OpenTelemetryRecorder recorder, ApplicationInfoBuildItem appInfo) {
        return SyntheticBeanBuildItem.configure(DelayedAttributes.class).types(Attributes.class)
                .supplier(recorder.delayedAttributes(Version.getVersion(),
                        appInfo.getName(), appInfo.getVersion()))
                .scope(Singleton.class)
                .setRuntimeInit()
                .done();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem kubernetesResource(OpenTelemetryRecorder recorder) {
        return SyntheticBeanBuildItem.configure(Resource.class)
                .types(Resource.class)
                .supplier(recorder.kubernetesResource())
                .scope(Singleton.class)
                .setRuntimeInit()
                .done();
    }

    @BuildStep
    void kubernetesEnvVars(
            List<KubernetesResourceMetadataBuildItem> resourceMetadata,
            ApplicationInfoBuildItem appInfo,
            BuildProducer<KubernetesEnvBuildItem> envProducer) {
        if (resourceMetadata.isEmpty()) {
            return;
        }
        // Downward API fieldRef-based env vars
        envProducer.produce(KubernetesEnvBuildItem.createFromField(NAMESPACE_ENV, "metadata.namespace", null));
        envProducer.produce(KubernetesEnvBuildItem.createFromField(POD_NAME_ENV, "metadata.name", null));
        envProducer.produce(KubernetesEnvBuildItem.createFromField(POD_UID_ENV, "metadata.uid", null));
        envProducer.produce(KubernetesEnvBuildItem.createFromField(NODE_NAME_ENV, "spec.nodeName", null));
        // Static env vars — values resolved from K8s extension metadata and application info
        envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(CONTAINER_NAME_ENV, appInfo.getName(), null));
        String deploymentName = resourceMetadata.stream()
                .findFirst()
                .map(KubernetesResourceMetadataBuildItem::getName)
                .orElse(appInfo.getName());
        envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(DEPLOYMENT_NAME_ENV, deploymentName, null));
    }

    @BuildStep
    RunTimeConfigurationDefaultBuildItem setTestModeShutdownWaitTime(LaunchModeBuildItem launchMode) {
        if (launchMode.getLaunchMode() == LaunchMode.TEST) {
            // Tests take a long time to shutdown
            return new RunTimeConfigurationDefaultBuildItem(
                    "quarkus.otel.experimental.shutdown-wait-time", "100ms");
        }
        return null;
    }

    @SuppressWarnings({ "deprecation", "removal" })
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void openTelemetryBean(OpenTelemetryRecorder recorder,
            CoreVertxBuildItem vertx,
            LaunchModeBuildItem launchMode,
            OTelBuildConfig oTelBuildConfig,
            CombinedIndexBuildItem index,
            BuildProducer<SyntheticBeanBuildItem> syntheticProducer,
            BuildProducer<ServiceStartBuildItem> serviceStart) {
        // Applications with user beans of this SPI keep the legacy AutoConfiguredOpenTelemetrySdk engine; the
        // recorder decides that from the resolvable beans. This scan only drives the warning, since it cannot see
        // every bean (e.g. one from a producer method). Removed together with the SPI in Quarkus 4.3.
        // Implementations of the internal marker interface are Quarkus wiring, not deprecated usage.
        Set<DotName> internalCustomizers = new HashSet<>();
        for (ClassInfo implementor : index.getIndex().getAllKnownImplementations(
                DotName.createSimple(InternalSdkBuilderCustomizer.class.getName()))) {
            internalCustomizers.add(implementor.name());
        }
        List<String> legacySdkBuilderCustomizers = new ArrayList<>();
        for (ClassInfo implementor : index.getIndex().getAllKnownImplementations(
                DotName.createSimple(AutoConfiguredOpenTelemetrySdkBuilderCustomizer.class.getName()))) {
            if (!Modifier.isAbstract(implementor.flags()) && !internalCustomizers.contains(implementor.name())) {
                legacySdkBuilderCustomizers.add(implementor.name().toString());
            }
        }
        if (!legacySdkBuilderCustomizers.isEmpty()) {
            LOG.warnf("The following AutoConfiguredOpenTelemetrySdkBuilderCustomizer implementations are deprecated "
                    + "and the SPI is scheduled for removal: %s. While they are present, the OpenTelemetry SDK is "
                    + "initialized with the legacy autoconfigure engine instead of the declarative configuration "
                    + "model. Migrate them to CDI beans (Sampler, SpanProcessor, MetricReader, Resource, ...) or "
                    + "the OpenTelemetry autoconfigure service provider interfaces.",
                    String.join(", ", legacySdkBuilderCustomizers));
        }

        // Each injection point must mirror a SyntheticCreationalContext.getInjectedReference(...) lookup in
        // OpenTelemetryRecorder.opentelemetryBean(), matching both the type and the qualifiers.
        AnnotationInstance any = AnnotationInstance.builder(Any.class).build();
        syntheticProducer.produce(SyntheticBeanBuildItem.configure(OpenTelemetry.class)
                .defaultBean()
                .setRuntimeInit()
                .unremovable()
                .scope(Singleton.class)
                .addInjectionPoint(ClassType.create(ApplicationConfig.class))
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(DelayedAttributes.class)),
                        any)
                // Instance injection points, not @All lists: every injection point of a synthetic bean is created
                // before the bean's function runs, so a List would instantiate the beans (starting e.g. a user
                // SpanProcessor's worker thread) even on the disabled-SDK and legacy paths, which never consume
                // them. An Instance creates beans only when iterated, inside the recorder's enablement gates, and
                // iterates in the same priority order as an @All list.
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
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(MetricReader.class)), any)
                // default-qualified, matching the legacy SimpleLogRecordProcessorCustomizer's Instance resolution
                .addInjectionPoint(ParameterizedType.create(Instance.class, ClassType.create(LogRecordExporter.class)))
                // The deprecated-type Instance drives the engine decision: the built-in customizers are @Typed to
                // the internal marker interface, so only user beans carry the deprecated type. Both Instances are
                // iterated only on the legacy engine path.
                .addInjectionPoint(ParameterizedType.create(Instance.class,
                        ClassType.create(AutoConfiguredOpenTelemetrySdkBuilderCustomizer.class)))
                .addInjectionPoint(ParameterizedType.create(Instance.class,
                        ClassType.create(InternalSdkBuilderCustomizer.class)))
                .createWith(recorder.opentelemetryBean())
                .destroyer(OpenTelemetryDestroyer.class)
                .done());

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT || launchMode.getLaunchMode() == LaunchMode.TEST) {
            recorder.resetGlobalOpenTelemetryForDevMode();
        }

        recorder.eagerlyCreateContextStorage();
        recorder.storeVertxOnContextStorage(vertx.getVertx());

        serviceStart.produce(new ServiceStartBuildItem("OpenTelemetry"));
    }

    // A separate step from openTelemetryBean: that step consumes CombinedIndexBuildItem, and extensions consuming
    // OpenTelemetrySdkBuildItem may contribute classes to the index (e.g. the MongoDB client's command listener),
    // which would otherwise create a build-step cycle.
    @SuppressWarnings({ "deprecation", "removal" })
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void openTelemetrySdkEnabled(OpenTelemetryRecorder recorder,
            OTelBuildConfig oTelBuildConfig,
            BuildProducer<OpenTelemetrySdkBuildItem> openTelemetrySdkBuildItemBuildProducer,
            BuildProducer<io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem> deprecatedOpenTelemetrySdkBuildItemBuildProducer) {
        // same as `TracerEnabled`
        boolean tracingEnabled = oTelBuildConfig.traces().enabled()
                .map(it -> it && oTelBuildConfig.enabled())
                .orElseGet(oTelBuildConfig::enabled);
        // same as `MetricProcessor.MetricEnabled`
        boolean metricsEnabled = oTelBuildConfig.metrics().enabled()
                .map(it -> it && oTelBuildConfig.enabled())
                .orElseGet(oTelBuildConfig::enabled);
        // same as `LogHandlerProcessor.LogsEnabled`
        boolean loggingEnabled = oTelBuildConfig.logs().enabled()
                .map(it -> it && oTelBuildConfig.enabled())
                .orElseGet(oTelBuildConfig::enabled);

        openTelemetrySdkBuildItemBuildProducer.produce(new OpenTelemetrySdkBuildItem(
                tracingEnabled, metricsEnabled, loggingEnabled, recorder.isOtelSdkEnabled()));
        deprecatedOpenTelemetrySdkBuildItemBuildProducer
                .produce(new io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem(
                        tracingEnabled, metricsEnabled, loggingEnabled, recorder.isOtelSdkEnabled()));
    }

    @BuildStep
    void handleServices(OTelBuildConfig config,
            BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<RemovedResourceBuildItem> removedResources,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeReinitialized) throws IOException {

        final List<String> spanExporterProviders = ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                SPI_ROOT + ConfigurableSpanExporterProvider.class.getName())
                .stream()
                .filter(p -> !OtlpSpanExporterProvider.class.getName().equals(p))
                .collect(toList()); // filter out OtlpSpanExporterProvider since it depends on OkHttp
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

        final List<String> metricExporterProviders = ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                SPI_ROOT + ConfigurableMetricExporterProvider.class.getName())
                .stream()
                .filter(p -> !OtlpMetricExporterProvider.class.getName().equals(p))
                .collect(toList()); // filter out OtlpMetricExporterProvider since it depends on OkHttp
        if (!metricExporterProviders.isEmpty()) {
            services.produce(
                    new ServiceProviderBuildItem(ConfigurableMetricExporterProvider.class.getName(), metricExporterProviders));
        }
        if (config.metrics().exporter().stream().noneMatch(ExporterType.Constants.OTLP_VALUE::equals)) {
            removedResources.produce(new RemovedResourceBuildItem(
                    ArtifactKey.fromString("io.opentelemetry:opentelemetry-exporter-otlp"),
                    Set.of("META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")));
        }

        final List<String> logRecordExporterProviders = ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                SPI_ROOT + ConfigurableLogRecordExporterProvider.class.getName())
                .stream()
                .filter(p -> !OtlpLogRecordExporterProvider.class.getName().equals(p))
                .collect(toList()); // filter out OtlpLogRecordExporterProvider since it depends on OkHttp
        if (!logRecordExporterProviders.isEmpty()) {
            services.produce(
                    new ServiceProviderBuildItem(ConfigurableLogRecordExporterProvider.class.getName(),
                            logRecordExporterProviders));
        }
        if (config.logs().exporter().stream().noneMatch(ExporterType.Constants.OTLP_VALUE::equals)) {
            removedResources.produce(new RemovedResourceBuildItem(
                    ArtifactKey.fromString("io.opentelemetry:opentelemetry-exporter-otlp"),
                    Set.of("META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider")));
        }

        runtimeReinitialized.produce(
                new RuntimeInitializedClassBuildItem("io.opentelemetry.sdk.autoconfigure.TracerProviderConfiguration"));
        runtimeReinitialized.produce(
                new RuntimeInitializedClassBuildItem("io.opentelemetry.sdk.autoconfigure.MeterProviderConfiguration"));
        runtimeReinitialized.produce(
                new RuntimeInitializedClassBuildItem("io.opentelemetry.sdk.autoconfigure.LoggerProviderConfiguration"));
        runtimeReinitialized.produce(
                new RuntimeInitializedClassBuildItem("io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogHandler"));

        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ConfigurableSamplerProvider.class.getName()));

        // Declarative-config components, resolved by name through the ComponentProvider SPI. The upstream OTLP
        // ComponentProviders depend on an HTTP sender the user has to add (e.g. OkHttp), so they are only
        // registered when an exporter is explicitly set to "otlp" — the otlp_http/otlp_grpc model shapes resolve
        // exclusively through them.
        final boolean otlpExporterRequested = Stream
                .of(config.traces().exporter(), config.metrics().exporter(), config.logs().exporter())
                .flatMap(List::stream)
                .anyMatch(ExporterType.Constants.OTLP_VALUE::equals);
        final List<String> componentProviders = ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                SPI_ROOT + ComponentProvider.class.getName())
                .stream()
                .filter(p -> otlpExporterRequested || !p.startsWith("io.opentelemetry.exporter.otlp."))
                .collect(toList());
        if (!componentProviders.isEmpty()) {
            services.produce(new ServiceProviderBuildItem(ComponentProvider.class.getName(), componentProviders));
        }
        // remove the service file that contains the upstream OTLP ComponentProviders, matching the
        // Configurable*ExporterProvider blocks above
        if (!otlpExporterRequested) {
            removedResources.produce(new RemovedResourceBuildItem(
                    ArtifactKey.fromString("io.opentelemetry:opentelemetry-exporter-otlp"),
                    Set.of("META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider")));
        }

        // The following are added but not officially supported, yet.
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                AutoConfigurationCustomizerProvider.class.getName()));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ResourceProvider.class.getName()));
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                ConfigurablePropagatorProvider.class.getName()));
    }

    // OTel's Marshaler uses JsonSerializer which references Jackson 2 (com.fasterxml.jackson.core).
    // Since we migrated to Jackson 3, rewrite Marshaler to use our Jackson3JsonSerializer instead.
    @BuildStep
    void rewriteMarshalerForJackson3(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        transformers.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform("io.opentelemetry.exporter.internal.marshal.Marshaler")
                .setCacheable(true)
                .setVisitorFunction((className, classVisitor) -> {
                    // Remove methods whose signatures reference Jackson 2 types (writeJsonToGenerator,
                    // writeJsonWithNewline) — Quarkus does not call them and the Jackson 2 classes are
                    // not on the classpath.
                    ClassVisitor methodRemover = new ClassVisitor(AsmUtil.ASM_API_VERSION, classVisitor) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                String signature, String[] exceptions) {
                            if (descriptor.contains("com/fasterxml/jackson")) {
                                return null;
                            }
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    };
                    // Remap JsonSerializer → Jackson3JsonSerializer so writeJsonTo(OutputStream)
                    // instantiates our Jackson 3 implementation.
                    Remapper remapper = new Remapper() {
                        @Override
                        public String map(String internalName) {
                            if ("io/opentelemetry/exporter/internal/marshal/JsonSerializer".equals(internalName)) {
                                return "io/opentelemetry/exporter/internal/marshal/Jackson3JsonSerializer";
                            }
                            return internalName;
                        }
                    };
                    return new ClassRemapper(methodRemover, remapper);
                })
                .build());
    }

    @BuildStep
    void registerOpenTelemetryContextStorage(
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.opentelemetry.context.ContextStorageProvider"));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(QuarkusContextStorage.class)
                .reason(getClass().getName())
                .methods().fields().build());
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
                                InterceptorBinding.of(AddingSpanAttributes.class, Set.of("value")),
                                InterceptorBinding.of(SPAN_METHOD, Set.of("declaringClass", "method")));
                    }
                }));

        additionalBeans.produce(new AdditionalBeanBuildItem(
                WithSpanInterceptor.class,
                AddingSpanAttributesInterceptor.class));
    }

    @BuildStep
    void transformWithSpan(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                .whenClass(c -> c.name().equals(WITH_SPAN_INTERCEPTOR) || c.name().equals(ADD_SPAN_ATTRIBUTES_INTERCEPTOR))
                .transform(ctx -> {
                    ClassInfo clazz = ctx.declaration().asClass();
                    if (clazz.name().equals(WITH_SPAN_INTERCEPTOR)) {
                        ctx.add(AnnotationInstance.builder(WithSpan.class).build());
                    } else if (clazz.name().equals(ADD_SPAN_ATTRIBUTES_INTERCEPTOR)) {
                        ctx.add(AnnotationInstance.builder(AddingSpanAttributes.class).build());
                    }
                })));

        // WITH_SPAN_INTERCEPTOR and ADD_SPAN_ATTRIBUTES must not be applied at the same time and the first has priority.
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forMethods()
                .whenAllMatch(WITH_SPAN, ADD_SPAN_ATTRIBUTES)
                .transform(ctx -> ctx.remove(isAddSpanAttribute))));
    }

    /**
     * The {@code $$CDIWrapper} generated for a REST client interface is the class that gets intercepted, so the span
     * name and the {@code code.function.name} attribute derived from the intercepted method would name the generated
     * wrapper. We resolve the REST client interface method at build time instead and pass it to the interceptor as an
     * interceptor binding, so both name the interface declaring the method.
     */
    @BuildStep
    void transformRestClientWithSpan(List<RegisteredRestClientBuildItem> restClients,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        if (restClients.isEmpty()) {
            return;
        }
        Map<DotName, RegisteredRestClientBuildItem> restClientsByWrapper = new HashMap<>();
        for (RegisteredRestClientBuildItem restClient : restClients) {
            restClientsByWrapper.put(restClient.getCdiWrapperName(), restClient);
        }
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forMethods()
                .whenMethod(m -> restClientsByWrapper.containsKey(m.declaringClass().name()))
                .transform(ctx -> {
                    if (!ctx.hasAnnotation(WITH_SPAN) || ctx.hasAnnotation(SPAN_METHOD)) {
                        return;
                    }
                    MethodInfo wrapperMethod = ctx.declaration().asMethod();
                    RegisteredRestClientBuildItem restClient = restClientsByWrapper.get(wrapperMethod.declaringClass().name());
                    MethodInfo interfaceMethod = findInterfaceMethod(restClient, wrapperMethod);
                    if (interfaceMethod == null) {
                        // not a method of the REST client interface, let the interceptor report the wrapper method
                        return;
                    }
                    ctx.add(AnnotationInstance.builder(SPAN_METHOD)
                            .add("declaringClass", interfaceMethod.declaringClass().name().toString())
                            .add("method", interfaceMethod.name())
                            .build());
                })));
    }

    /**
     * @return the method of the REST client interface, or of one of its super interfaces, that the given method of the
     *         generated CDI wrapper implements, or {@code null} if there is none
     */
    private static MethodInfo findInterfaceMethod(RegisteredRestClientBuildItem restClient, MethodInfo wrapperMethod) {
        for (MethodInfo candidate : restClient.getCdiWrapperMethods()) {
            if (candidate.name().equals(wrapperMethod.name()) && sameParameterTypes(candidate, wrapperMethod)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Compares the erased parameter types of two methods.
     */
    private static boolean sameParameterTypes(MethodInfo a, MethodInfo b) {
        if (a.parametersCount() != b.parametersCount()) {
            return false;
        }
        for (int i = 0; i < a.parametersCount(); i++) {
            if (!a.parameterType(i).name().equals(b.parameterType(i).name())) {
                return false;
            }
        }
        return true;
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
        boolean redisClientAvailable = capabilities.isPresent(Capability.REDIS_CLIENT);
        boolean grpcAvailable = capabilities.isPresent(Capability.GRPC);
        recorder.setupVertxTracer(beanContainerBuildItem.getValue(), sqlClientAvailable, redisClientAvailable,
                grpcAvailable);
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
            ConfigValue explicitlyConfiguredDriverValue = Config.get().getConfigValue(driverPropertyKey);
            if (explicitlyConfiguredDriverValue.getValue() != null) {
                return explicitlyConfiguredDriverValue.getValue().equals(OPEN_TELEMETRY_DRIVER);
            }
        }
        return false;
    }

}
