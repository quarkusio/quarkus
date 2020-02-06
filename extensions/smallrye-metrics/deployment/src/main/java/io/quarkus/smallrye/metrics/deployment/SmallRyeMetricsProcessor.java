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
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.TIMER_INTERFACE;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
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
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.metrics.deployment.jandex.JandexBeanInfoAdapter;
import io.quarkus.smallrye.metrics.deployment.jandex.JandexMemberInfoAdapter;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;
import io.quarkus.smallrye.metrics.runtime.MetadataHolder;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsRecorder;
import io.quarkus.smallrye.metrics.runtime.TagHolder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.smallrye.metrics.MetricProducer;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.interceptors.ConcurrentGaugeInterceptor;
import io.smallrye.metrics.interceptors.CountedInterceptor;
import io.smallrye.metrics.interceptors.MeteredInterceptor;
import io.smallrye.metrics.interceptors.MetricNameFactory;
import io.smallrye.metrics.interceptors.MetricsInterceptor;
import io.smallrye.metrics.interceptors.TimedInterceptor;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class SmallRyeMetricsProcessor {
    private static final Logger LOGGER = Logger.getLogger("io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsProcessor");

    @ConfigRoot(name = "smallrye-metrics")
    static final class SmallRyeMetricsConfig {

        /**
         * The path to the metrics handler.
         */
        @ConfigItem(defaultValue = "/metrics")
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
        @ConfigItem(name = "micrometer.compatibility", defaultValue = "false")
        public boolean micrometerCompatibility;

    }

    SmallRyeMetricsConfig metrics;

    @BuildStep
    @Record(STATIC_INIT)
    void createRoute(BuildProducer<RouteBuildItem> routes,
            SmallRyeMetricsRecorder recorder,
            HttpRootPathBuildItem httpRoot,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            LaunchModeBuildItem launchModeBuildItem) {
        Function<Router, Route> route = recorder.route(metrics.path + (metrics.path.endsWith("/") ? "*" : "/*"));
        Function<Router, Route> slash = recorder.route(metrics.path);

        // add metrics endpoint for not found display in dev or test mode
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(metrics.path));
        }
        routes.produce(new RouteBuildItem(route, recorder.handler(httpRoot.adjustPath(metrics.path)), HandlerType.BLOCKING));
        routes.produce(new RouteBuildItem(slash, recorder.handler(httpRoot.adjustPath(metrics.path)), HandlerType.BLOCKING));
    }

    @BuildStep
    void beans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(MetricProducer.class,
                MetricNameFactory.class,
                MetricRegistries.class,
                MetricsInterceptor.class,
                MeteredInterceptor.class,
                ConcurrentGaugeInterceptor.class,
                CountedInterceptor.class,
                TimedInterceptor.class,
                MetricsRequestHandler.class));
        unremovableBeans.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(MetricsRequestHandler.class.getName())));
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformBeanScope(BeanArchiveIndexBuildItem index) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public int getPriority() {
                // this specifically should run after the JAX-RS AnnotationTransformers
                return BuildExtension.DEFAULT_PRIORITY - 100;
            }

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.isClass()) {
                    if (BuiltinScope.isIn(ctx.getAnnotations())) {
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
            }
        });
    }

    @BuildStep
    void annotationTransformers(BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        // attach @MetricsBinding to each class that contains any metric annotations
        transformers.produce(new AnnotationsTransformerBuildItem(ctx -> {
            if (ctx.isClass()) {
                // skip classes in package io.smallrye.metrics.interceptors
                ClassInfo clazz = ctx.getTarget().asClass();
                if (clazz.name().toString()
                        .startsWith(io.smallrye.metrics.interceptors.MetricsInterceptor.class.getPackage().getName())) {
                    return;
                }

                if (clazz.annotations().keySet().contains(GAUGE)) {
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
                    ctx.transform().add(AnnotationInstance.create(METRICS_BINDING,
                            ctx.getTarget(), new AnnotationValue[0]))
                            .done();
                }
            }
        }));
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
    public CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.METRICS);
    }

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_METRICS);
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
            metrics.registerBaseMetrics(shutdown);
            metrics.registerVendorMetrics(shutdown);
        }
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("io.smallrye.metrics.MetricsRegistryImpl",
                "Register metric ["));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void registerMetricsFromAnnotatedMethods(SmallRyeMetricsRecorder metrics,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();
        JandexBeanInfoAdapter beanInfoAdapter = new JandexBeanInfoAdapter(index);
        JandexMemberInfoAdapter memberInfoAdapter = new JandexMemberInfoAdapter(index);

        Set<MethodInfo> collectedMetricsMethods = new HashSet<>();
        Map<DotName, ClassInfo> collectedMetricsClasses = new HashMap<>();

        for (DotName metricAnnotation : METRICS_ANNOTATIONS) {
            Collection<AnnotationInstance> metricAnnotationInstances = index.getAnnotations(metricAnnotation);
            for (AnnotationInstance metricAnnotationInstance : metricAnnotationInstances) {
                AnnotationTarget metricAnnotationTarget = metricAnnotationInstance.target();
                switch (metricAnnotationTarget.kind()) {
                    case METHOD: {
                        MethodInfo method = metricAnnotationTarget.asMethod();
                        if (!method.declaringClass().name().toString().startsWith("io.smallrye.metrics")) {
                            collectedMetricsMethods.add(method);
                        }
                        break;
                    }
                    case CLASS: {
                        ClassInfo clazz = metricAnnotationTarget.asClass();
                        if (!clazz.name().toString().startsWith("io.smallrye.metrics")) {
                            collectMetricsClassAndSubClasses(index, collectedMetricsClasses, clazz);
                        }
                        break;
                    }
                }
            }
        }

        for (ClassInfo clazz : collectedMetricsClasses.values()) {
            BeanInfo beanInfo = beanInfoAdapter.convert(clazz);
            ClassInfo superclass = clazz;
            // register metrics for all inherited methods as well
            while (superclass != null && superclass.superName() != null) {
                for (MethodInfo method : superclass.methods()) {
                    // if we're looking at a superclass, skip methods that are overridden by the subclass
                    if (superclass != clazz) {
                        if (clazz.method(method.name(), method.parameters().toArray(new Type[] {})) != null) {
                            continue;
                        }
                    }
                    if (!Modifier.isPrivate(method.flags())) {
                        metrics.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
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
    void registerMetricsFromProducers(
            SmallRyeMetricsRecorder recorder,
            ValidationPhaseBuildItem validationPhase,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();
        for (io.quarkus.arc.processor.BeanInfo bean : validationPhase.getContext().beans().producers()) {
            MetricType metricType = getMetricType(bean.getImplClazz());
            if (metricType != null) {
                AnnotationTarget target = bean.getTarget().get();
                AnnotationInstance metricAnnotation = null;
                String memberName = null;
                if (bean.isProducerField()) {
                    FieldInfo field = target.asField();
                    metricAnnotation = field.annotation(METRIC);
                    memberName = field.name();
                }
                if (bean.isProducerMethod()) {
                    MethodInfo method = target.asMethod();
                    metricAnnotation = method.annotation(METRIC);
                    memberName = method.name();
                }
                if (metricAnnotation != null) {
                    String nameValue = metricAnnotation.valueWithDefault(index, "name").asString();
                    boolean absolute = metricAnnotation.valueWithDefault(index, "absolute").asBoolean();
                    String metricSimpleName = !nameValue.isEmpty() ? nameValue : memberName;
                    String declaringClassName = bean.getDeclaringBean().getImplClazz().name().toString();
                    String metricsFinalName = absolute ? metricSimpleName
                            : MetricRegistry.name(declaringClassName, metricSimpleName);
                    recorder.registerMetricFromProducer(
                            bean.getIdentifier(),
                            metricType,
                            metricsFinalName,
                            metricAnnotation.valueWithDefault(index, "tags").asStringArray(),
                            metricAnnotation.valueWithDefault(index, "description").asString(),
                            metricAnnotation.valueWithDefault(index, "displayName").asString(),
                            metricAnnotation.valueWithDefault(index, "unit").asString());
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
            ShutdownContextBuildItem shutdown,
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
                    recorder.registerMetric(MetricRegistry.Type.VENDOR,
                            MetadataHolder.from(additionalMetric.getMetadata()),
                            tags,
                            additionalMetric.getImplementor(),
                            shutdown);
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
