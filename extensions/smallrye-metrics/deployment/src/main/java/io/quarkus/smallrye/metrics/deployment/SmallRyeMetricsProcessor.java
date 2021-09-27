package io.quarkus.smallrye.metrics.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.CONCURRENT_GAUGE_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.COUNTER_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.GAUGE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.GAUGE_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.HISTOGRAM_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METER_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METRIC;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METRICS_ANNOTATIONS;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METRICS_BINDING;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METRIC_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.SIMPLE_TIMER_INTERFACE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.TIMER_INTERFACE;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.metrics.deployment.jandex.JandexBeanInfoAdapter;
import io.quarkus.smallrye.metrics.deployment.jandex.JandexMemberInfoAdapter;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricsConfigurationBuildItem;
import io.quarkus.smallrye.metrics.runtime.MetadataHolder;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsRecorder;
import io.quarkus.smallrye.metrics.runtime.TagHolder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.smallrye.metrics.MetricProducer;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.interceptors.ConcurrentGaugeInterceptor;
import io.smallrye.metrics.interceptors.CountedInterceptor;
import io.smallrye.metrics.interceptors.GaugeRegistrationInterceptor;
import io.smallrye.metrics.interceptors.MeteredInterceptor;
import io.smallrye.metrics.interceptors.MetricNameFactory;
import io.smallrye.metrics.interceptors.MetricsBinding;
import io.smallrye.metrics.interceptors.SimplyTimedInterceptor;
import io.smallrye.metrics.interceptors.TimedInterceptor;

public class SmallRyeMetricsProcessor {
    static final Logger LOGGER = Logger.getLogger("io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsProcessor");

    @ConfigRoot(name = "smallrye-metrics")
    static final class SmallRyeMetricsConfig {

        /**
         * The path to the metrics handler.
         */
        @ConfigItem(defaultValue = "metrics")
        String path;

        /**
         * Whether or not metrics published by Quarkus extensions should be enabled.
         */
        @ConfigItem(name = "extensions.enabled", defaultValue = "true")
        public boolean extensionsEnabled;

        /**
         * Apply Micrometer compatibility mode, where instead of regular 'base' and 'vendor' metrics,
         * Quarkus exposes the same 'jvm' metrics that Micrometer does. Application metrics are unaffected by this mode.
         * The use case is to facilitate migration from Micrometer-based metrics, because original dashboards for JVM metrics
         * will continue working without having to rewrite them.
         */
        @ConfigItem(name = "micrometer.compatibility")
        public boolean micrometerCompatibility;

        /**
         * Whether or not detailed JAX-RS metrics should be enabled.
         * <p>
         * See <a href=
         * "https://github.com/eclipse/microprofile-metrics/blob/2.3.x/spec/src/main/asciidoc/required-metrics.adoc#optional-rest">MicroProfile
         * Metrics: Optional REST metrics</a>.
         */
        @ConfigItem(name = "jaxrs.enabled", defaultValue = "false")
        public boolean jaxrsEnabled;
    }

    SmallRyeMetricsConfig metrics;

    @BuildStep
    MetricsConfigurationBuildItem metricsConfigurationBuildItem() {
        return new MetricsConfigurationBuildItem(metrics.path);
    }

    @BuildStep
    MetricsCapabilityBuildItem metricsCapabilityBuildItem(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        if (metrics.extensionsEnabled) {
            return new MetricsCapabilityBuildItem(MetricsFactory.MP_METRICS::equals,
                    nonApplicationRootPathBuildItem.resolvePath(metrics.path));
        }
        return null;
    }

    @BuildStep
    @Record(STATIC_INIT)
    void createRoute(BuildProducer<RouteBuildItem> routes,
            SmallRyeMetricsRecorder recorder,
            NonApplicationRootPathBuildItem frameworkRoot,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            LaunchModeBuildItem launchModeBuildItem,
            BeanContainerBuildItem beanContainer) {

        // add metrics endpoint for not found display in dev or test mode
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(metrics.path));
        }
        routes.produce(frameworkRoot.routeBuilder()
                .route(metrics.path + (metrics.path.endsWith("/") ? "*" : "/*"))
                .handler(recorder.handler(frameworkRoot.resolvePath(metrics.path)))
                .blockingRoute()
                .build());
        routes.produce(frameworkRoot.routeBuilder()
                .route(metrics.path)
                .handler(recorder.handler(frameworkRoot.resolvePath(metrics.path)))
                .displayOnNotFoundPage("Metrics")
                .blockingRoute()
                .build());
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(new AdditionalBeanBuildItem(MetricProducer.class,
                MetricNameFactory.class,
                GaugeRegistrationInterceptor.class,
                MeteredInterceptor.class,
                ConcurrentGaugeInterceptor.class,
                CountedInterceptor.class,
                TimedInterceptor.class,
                SimplyTimedInterceptor.class));
        // MetricsRequestHandler and MetricRegistries are looked up programmatically in the recorder
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(MetricsRequestHandler.class, MetricRegistries.class).build());
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformBeanScope(BeanArchiveIndexBuildItem index,
            CustomScopeAnnotationsBuildItem scopes) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public int getPriority() {
                // this specifically should run after the JAX-RS AnnotationTransformers
                return BuildExtension.DEFAULT_PRIORITY - 100;
            }

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (scopes.isScopeIn(ctx.getAnnotations())) {
                    return;
                }
                ClassInfo clazz = ctx.getTarget().asClass();
                if (!isJaxRsEndpoint(clazz) && !isJaxRsProvider(clazz)) {
                    while (clazz != null && clazz.superName() != null) {
                        Map<DotName, List<AnnotationInstance>> annotations = clazz.annotations();
                        if (annotations.containsKey(GAUGE)
                                || annotations.containsKey(SmallRyeMetricsDotNames.CONCURRENT_GAUGE)
                                || annotations.containsKey(SmallRyeMetricsDotNames.COUNTED)
                                || annotations.containsKey(SmallRyeMetricsDotNames.METERED)
                                || annotations.containsKey(SmallRyeMetricsDotNames.SIMPLY_TIMED)
                                || annotations.containsKey(SmallRyeMetricsDotNames.TIMED)
                                || annotations.containsKey(SmallRyeMetricsDotNames.METRIC)) {
                            LOGGER.debugf(
                                    "Found metrics business methods on a class %s with no scope defined - adding @Dependent",
                                    ctx.getTarget());
                            ctx.transform().add(Dependent.class).done();
                            break;
                        }
                        clazz = index.getIndex().getClassByName(clazz.superName());
                    }
                }
            }
        });
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformers() {
        // attach @MetricsBinding to each class that contains any metric annotations
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                // skip classes in package io.smallrye.metrics.interceptors
                ClassInfo clazz = context.getTarget().asClass();
                if (clazz.name().toString()
                        .startsWith(
                                io.smallrye.metrics.interceptors.GaugeRegistrationInterceptor.class.getPackage().getName())) {
                    return;
                }
                if (clazz.annotations().containsKey(GAUGE)) {
                    BuiltinScope beanScope = BuiltinScope.from(clazz);
                    if (!isJaxRsEndpoint(clazz) && beanScope != null &&
                            !beanScope.equals(BuiltinScope.APPLICATION) &&
                            !beanScope.equals(BuiltinScope.SINGLETON)) {
                        LOGGER.warnf("Bean %s declares a org.eclipse.microprofile.metrics.annotation.Gauge " +
                                "but is of a scope that typically " +
                                "creates multiple instances. Gauges are forbidden on beans " +
                                "that create multiple instances, this will cause errors " +
                                "when constructing them. Please use annotated gauges only in beans with " +
                                "@ApplicationScoped or @Singleton scopes, or in JAX-RS endpoints.",
                                clazz.name().toString());
                    }
                    context.transform().add(MetricsBinding.class).done();
                }
            }

        });
    }

    /**
     * Methods with a @Gauge annotation need to be registered for reflection because
     * gauges are registered at runtime and the registering interceptor must be able to see
     * the annotation.
     */
    @BuildStep
    void reflectiveMethodsWithGauges(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {
        for (AnnotationInstance annotation : beanArchiveIndex.getIndex().getAnnotations(GAUGE)) {
            if (annotation.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(annotation.target().asMethod()));
            }
        }
    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectMetric() {
        return new AutoInjectAnnotationBuildItem(SmallRyeMetricsDotNames.METRIC);
    }

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_METRICS);
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(BeanContainerBuildItem beanContainerBuildItem,
            SmallRyeMetricsRecorder metrics,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (DotName metricsAnnotation : METRICS_ANNOTATIONS) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, metricsAnnotation.toString()));
        }

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, METRICS_BINDING.toString()));
        metrics.createRegistries(beanContainerBuildItem.getValue());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerBaseAndVendorMetrics(SmallRyeMetricsRecorder metrics,
            ShutdownContextBuildItem shutdown,
            SmallRyeMetricsConfig config) {
        if (config.micrometerCompatibility) {
            metrics.registerMicrometerJvmMetrics(shutdown);
        } else {
            metrics.registerBaseMetrics();
            metrics.registerVendorMetrics();
        }
    }

    /**
     * When shutting down, drop all metric registries. Specifically in dev mode,
     * this is to ensure all metrics start from zero after a reload, and that extensions
     * don't have to deregister their own metrics manually.
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void dropRegistriesAtShutdown(SmallRyeMetricsRecorder recorder,
            ShutdownContextBuildItem shutdown) {
        recorder.dropRegistriesAtShutdown(shutdown);
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("io.smallrye.metrics.MetricsRegistryImpl",
                "Register metric ["));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void registerMetricsFromAnnotatedMethods(SmallRyeMetricsRecorder metrics,
            BeanArchiveIndexBuildItem beanArchiveIndex, TransformedAnnotationsBuildItem transformedAnnotations,
            ValidationPhaseBuildItem validationPhase) {
        IndexView index = beanArchiveIndex.getIndex();
        JandexBeanInfoAdapter beanInfoAdapter = new JandexBeanInfoAdapter(index, transformedAnnotations);
        JandexMemberInfoAdapter memberInfoAdapter = new JandexMemberInfoAdapter(index, transformedAnnotations);

        Set<MethodInfo> collectedMetricsMethods = new HashSet<>();
        Map<DotName, ClassInfo> collectedMetricsClasses = new HashMap<>();

        // find stereotypes that contain metric annotations so we can include them in the search
        Set<DotName> metricAndStereotypeAnnotations = new HashSet<>();
        metricAndStereotypeAnnotations.addAll(METRICS_ANNOTATIONS);
        for (ClassInfo candidate : beanArchiveIndex.getIndex().getKnownClasses()) {
            if (transformedAnnotations.getAnnotation(candidate, DotNames.STEREOTYPE) != null &&
                    transformedAnnotations.getAnnotations(candidate).stream()
                            .anyMatch(SmallRyeMetricsDotNames::isMetricAnnotation)) {
                metricAndStereotypeAnnotations.add(candidate.name());
            }
        }

        // First add all class beans with a SmallRye Metrics interceptor associated
        validationPhase.getContext().beans().classBeans().filter(this::hasMetricsInterceptorAssociated)
                .forEach(b -> collectMetricsClassAndSubClasses(index, collectedMetricsClasses, b.getTarget().get().asClass()));

        for (DotName metricAnnotation : metricAndStereotypeAnnotations) {
            Collection<AnnotationInstance> metricAnnotationInstances = index.getAnnotations(metricAnnotation);
            for (AnnotationInstance metricAnnotationInstance : metricAnnotationInstances) {
                AnnotationTarget metricAnnotationTarget = metricAnnotationInstance.target();
                switch (metricAnnotationTarget.kind()) {
                    case METHOD:
                        MethodInfo method = metricAnnotationTarget.asMethod();
                        if (!method.declaringClass().name().toString().startsWith("io.smallrye.metrics")) {
                            if (!Modifier.isPrivate(method.flags())) {
                                collectedMetricsMethods.add(method);
                            } else {
                                LOGGER.warn("Private method is annotated with a metric: " + method +
                                        " in class " + method.declaringClass().name() + ". Metrics " +
                                        "are not collected for private methods. To enable metrics for this method, make " +
                                        "it at least package-private.");
                            }
                        }
                        break;
                    case CLASS:
                        ClassInfo clazz = metricAnnotationTarget.asClass();
                        if (!clazz.name().toString().startsWith("io.smallrye.metrics")) {
                            collectMetricsClassAndSubClasses(index, collectedMetricsClasses, clazz);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        for (ClassInfo clazz : collectedMetricsClasses.values()) {
            BeanInfo beanInfo = beanInfoAdapter.convert(clazz);
            ClassInfo superclass = clazz;
            Set<String> alreadyRegisteredNames = new HashSet<>();
            // register metrics for all inherited methods as well
            while (superclass != null && superclass.superName() != null) {
                for (MethodInfo method : superclass.methods()) {
                    if (!Modifier.isPrivate(method.flags()) && !alreadyRegisteredNames.contains(method.toString())) {
                        metrics.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
                        alreadyRegisteredNames.add(method.toString());
                    }
                }
                superclass = index.getClassByName(superclass.superName());
            }
            superclass = clazz;
            while (superclass != null && superclass.superName() != null) {
                // find inherited default methods which are not overridden by the original bean
                for (Type interfaceType : superclass.interfaceTypes()) {
                    ClassInfo ifaceInfo = beanArchiveIndex.getIndex().getClassByName(interfaceType.name());
                    if (ifaceInfo != null) {
                        findNonOverriddenDefaultMethods(ifaceInfo, alreadyRegisteredNames, metrics, beanArchiveIndex,
                                memberInfoAdapter,
                                beanInfo);
                    }
                }
                superclass = index.getClassByName(superclass.superName());
            }
        }

        for (MethodInfo method : collectedMetricsMethods) {
            ClassInfo declaringClazz = method.declaringClass();
            if (!collectedMetricsClasses.containsKey(declaringClazz.name())) {
                BeanInfo beanInfo = beanInfoAdapter.convert(declaringClazz);
                metrics.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
            }
        }
    }

    private boolean hasMetricsInterceptorAssociated(io.quarkus.arc.processor.BeanInfo bean) {
        if (!bean.hasAroundInvokeInterceptors()) {
            return false;
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            if (interceptor.getBeanClass().toString().startsWith("io.smallrye.metrics.interceptors")) {
                return true;
            }
        }
        return false;
    }

    private void findNonOverriddenDefaultMethods(ClassInfo interfaceInfo, Set<String> alreadyRegisteredNames,
            SmallRyeMetricsRecorder recorder,
            BeanArchiveIndexBuildItem beanArchiveIndex, JandexMemberInfoAdapter memberInfoAdapter, BeanInfo beanInfo) {
        // Check for default methods which are NOT overridden by the bean that we are registering metrics for
        // or any of its superclasses. Register a metric for each of them.
        for (MethodInfo method : interfaceInfo.methods()) {
            if (!Modifier.isAbstract(method.flags())) { // only take default methods
                if (!alreadyRegisteredNames.contains(method.toString())) {
                    recorder.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
                    alreadyRegisteredNames.add(method.toString());
                }
            }
        }
        // recursively repeat the same for interfaces which this interface extends
        for (Type extendedInterface : interfaceInfo.interfaceTypes()) {
            ClassInfo extendedInterfaceInfo = beanArchiveIndex.getIndex().getClassByName(extendedInterface.name());
            if (extendedInterfaceInfo != null) {
                findNonOverriddenDefaultMethods(extendedInterfaceInfo, alreadyRegisteredNames, recorder, beanArchiveIndex,
                        memberInfoAdapter,
                        beanInfo);
            }
        }
    }

    /**
     * Mark metric producer methods and fields as unremovable, they should be kept even if
     * there is no injection point for them.
     */
    @BuildStep
    void unremovableProducers(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        Type type = Type.create(METRIC_INTERFACE, Type.Kind.CLASS);
        unremovable.produce(
                new UnremovableBeanBuildItem(new Predicate<io.quarkus.arc.processor.BeanInfo>() {
                    @Override
                    public boolean test(io.quarkus.arc.processor.BeanInfo beanInfo) {
                        io.quarkus.arc.processor.BeanInfo declaringBean = beanInfo.getDeclaringBean();
                        return (beanInfo.isProducerMethod() || beanInfo.isProducerField())
                                && beanInfo.getTypes().contains(type)
                                && !declaringBean.getBeanClass().toString().startsWith("io.smallrye.metrics");
                    }
                }));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void registerRuntimeExtensionMetrics(
            SmallRyeMetricsRecorder recorder,
            List<MetricsFactoryConsumerBuildItem> metricsFactoryConsumerBuildItems) {
        for (MetricsFactoryConsumerBuildItem item : metricsFactoryConsumerBuildItems) {
            if (item.executionTime() == RUNTIME_INIT) {
                recorder.registerMetrics(item.getConsumer());
            }
        }
    }

    @BuildStep
    public void warnAboutMetricsFromProducers(ValidationPhaseBuildItem validationPhase,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> unused) {
        for (io.quarkus.arc.processor.BeanInfo bean : validationPhase.getContext().beans().producers()) {
            ClassInfo implClazz = bean.getImplClazz();
            if (implClazz == null) {
                continue;
            }
            MetricType metricType = getMetricType(implClazz);
            if (metricType != null) {
                AnnotationTarget target = bean.getTarget().get();
                AnnotationInstance metricAnnotation = null;
                if (bean.isProducerField()) {
                    FieldInfo field = target.asField();
                    metricAnnotation = field.annotation(METRIC);
                }
                if (bean.isProducerMethod()) {
                    MethodInfo method = target.asMethod();
                    metricAnnotation = method.annotation(METRIC);
                }
                if (metricAnnotation != null) {
                    LOGGER.warn(
                            "Metrics created from CDI producers are no longer supported. There will be no metric automatically registered "
                                    +
                                    "for producer " + target);
                }
            }
        }
    }

    /**
     * Register metrics required by other Quarkus extensions.
     */
    @BuildStep
    @Record(STATIC_INIT)
    void extensionMetrics(SmallRyeMetricsRecorder recorder,
            List<MetricBuildItem> additionalMetrics,
            List<MetricsFactoryConsumerBuildItem> metricsFactoryConsumerBuildItems,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        if (metrics.extensionsEnabled) {
            if (!additionalMetrics.isEmpty()) {
                unremovableBeans.produce(new UnremovableBeanBuildItem(
                        new UnremovableBeanBuildItem.BeanClassNameExclusion(MetricRegistry.class.getName())));
                unremovableBeans.produce(new UnremovableBeanBuildItem(
                        new UnremovableBeanBuildItem.BeanClassNameExclusion(MetricRegistries.class.getName())));
            }
            for (MetricBuildItem additionalMetric : additionalMetrics) {
                if (additionalMetric.isEnabled()) {
                    TagHolder[] tags = Arrays.stream(additionalMetric.getTags())
                            .map(TagHolder::from)
                            .toArray(TagHolder[]::new);
                    recorder.registerMetric(additionalMetric.getRegistryType(),
                            MetadataHolder.from(additionalMetric.getMetadata()),
                            tags,
                            additionalMetric.getImplementor());
                }
            }

            for (MetricsFactoryConsumerBuildItem item : metricsFactoryConsumerBuildItems) {
                if (item.executionTime() == STATIC_INIT) {
                    recorder.registerMetrics(item.getConsumer());
                }
            }
        }
    }

    /**
     * Obtains the MetricType from a bean that is a producer method or field,
     * or null if no MetricType can be detected.
     */
    private MetricType getMetricType(ClassInfo clazz) {
        DotName name = clazz.name();
        if (name.equals(GAUGE_INTERFACE)) {
            return MetricType.GAUGE;
        }
        if (name.equals(COUNTER_INTERFACE)) {
            return MetricType.COUNTER;
        }
        if (name.equals(CONCURRENT_GAUGE_INTERFACE)) {
            return MetricType.CONCURRENT_GAUGE;
        }
        if (name.equals(HISTOGRAM_INTERFACE)) {
            return MetricType.HISTOGRAM;
        }
        if (name.equals(SIMPLE_TIMER_INTERFACE)) {
            return MetricType.SIMPLE_TIMER;
        }
        if (name.equals(TIMER_INTERFACE)) {
            return MetricType.TIMER;
        }
        if (name.equals(METER_INTERFACE)) {
            return MetricType.METERED;
        }
        return null;
    }

    private void collectMetricsClassAndSubClasses(IndexView index, Map<DotName, ClassInfo> collectedMetricsClasses,
            ClassInfo clazz) {
        if (collectedMetricsClasses.containsKey(clazz.name())) {
            return;
        }
        collectedMetricsClasses.put(clazz.name(), clazz);
        for (ClassInfo subClass : index.getAllKnownSubclasses(clazz.name())) {
            collectedMetricsClasses.put(subClass.name(), subClass);
        }
    }

    private boolean isJaxRsEndpoint(ClassInfo clazz) {
        return clazz.annotations().containsKey(SmallRyeMetricsDotNames.JAXRS_PATH) ||
                clazz.annotations().containsKey(SmallRyeMetricsDotNames.REST_CONTROLLER);
    }

    private boolean isJaxRsProvider(ClassInfo clazz) {
        return clazz.annotations().containsKey(SmallRyeMetricsDotNames.JAXRS_PROVIDER);
    }

}
