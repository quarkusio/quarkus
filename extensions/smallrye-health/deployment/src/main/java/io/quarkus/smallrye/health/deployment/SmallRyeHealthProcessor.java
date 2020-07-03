package io.quarkus.smallrye.health.deployment;

import static io.quarkus.arc.processor.Annotations.containsAny;
import static io.quarkus.arc.processor.Annotations.getAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.health.runtime.ShutdownReadinessListener;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthGroupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRecorder;
import io.quarkus.smallrye.health.runtime.SmallRyeIndividualHealthGroupHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeLivenessHandler;
import io.quarkus.smallrye.health.runtime.SmallRyeReadinessHandler;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.health.HealthGroup;
import io.smallrye.health.HealthGroups;
import io.smallrye.health.SmallRyeHealthReporter;

class SmallRyeHealthProcessor {

    private static final DotName HEALTH = DotName.createSimple(Health.class.getName());

    private static final DotName LIVENESS = DotName.createSimple(Liveness.class.getName());

    private static final DotName READINESS = DotName.createSimple(Readiness.class.getName());

    private static final DotName HEALTH_GROUP = DotName.createSimple(HealthGroup.class.getName());

    private static final DotName HEALTH_GROUPS = DotName.createSimple(HealthGroups.class.getName());

    /**
     * The configuration for health checking.
     */
    SmallRyeHealthConfig health;

    HealthBuildTimeConfig config;

    @ConfigRoot(name = "smallrye-health")
    static final class SmallRyeHealthConfig {
        /**
         * Root path for health-checking endpoints.
         */
        @ConfigItem(defaultValue = "/health")
        String rootPath;

        /**
         * The relative path of the liveness health-checking endpoint.
         */
        @ConfigItem(defaultValue = "/live")
        String livenessPath;

        /**
         * The relative path of the readiness health-checking endpoint.
         */
        @ConfigItem(defaultValue = "/ready")
        String readinessPath;

        /**
         * The relative path of the health group endpoint.
         */
        @ConfigItem(defaultValue = "/group")
        String groupPath;
    }

    @BuildStep
    void healthCheck(BuildProducer<AdditionalBeanBuildItem> buildItemBuildProducer,
            List<HealthBuildItem> healthBuildItems) {
        boolean extensionsEnabled = config.extensionsEnabled &&
                !ConfigProvider.getConfig().getOptionalValue("mp.health.disable-default-procedures", boolean.class)
                        .orElse(false);
        if (extensionsEnabled) {
            for (HealthBuildItem buildItem : healthBuildItems) {
                if (buildItem.isEnabled()) {
                    buildItemBuildProducer.produce(new AdditionalBeanBuildItem(buildItem.getHealthCheckClass()));
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    void build(SmallRyeHealthRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            LaunchModeBuildItem launchModeBuildItem) throws IOException, ClassNotFoundException {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_HEALTH));

        // add health endpoints to not found page
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.livenessPath));
            displayableEndpoints
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.readinessPath));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.groupPath));
        }

        // Discover the beans annotated with @Health, @Liveness, @Readiness, @HealthGroup and @HealthGroups even if no scope is defined
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(LIVENESS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(READINESS));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUP));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUPS));

        // Add additional beans
        additionalBean.produce(new AdditionalBeanBuildItem(SmallRyeHealthReporter.class));

        // Make ArC discover @HealthGroup as a qualifier
        additionalBean.produce(new AdditionalBeanBuildItem(HealthGroup.class));

        // Discover and register the HealthCheckResponseProvider
        Set<String> providers = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + HealthCheckResponseProvider.class.getName());

        if (providers.isEmpty()) {
            throw new IllegalStateException("No HealthCheckResponseProvider implementation found.");
        } else if (providers.size() > 1) {
            throw new IllegalStateException(
                    String.format("Multiple HealthCheckResponseProvider implementations found: %s", providers));
        }

        final String provider = providers.iterator().next();
        final Class<? extends HealthCheckResponseProvider> responseProvider = (Class<? extends HealthCheckResponseProvider>) Class
                .forName(provider, true, Thread.currentThread().getContextClassLoader());
        recorder.registerHealthCheckResponseProvider(responseProvider);
    }

    @BuildStep
    public void defineHealthRoutes(BuildProducer<RouteBuildItem> routes,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();

        // Register the health handler
        routes.produce(new RouteBuildItem(health.rootPath, new SmallRyeHealthHandler(), HandlerType.BLOCKING));

        // Register the liveness handler
        routes.produce(
                new RouteBuildItem(health.rootPath + health.livenessPath, new SmallRyeLivenessHandler(),
                        HandlerType.BLOCKING));

        // Register the readiness handler
        routes.produce(
                new RouteBuildItem(health.rootPath + health.readinessPath, new SmallRyeReadinessHandler(),
                        HandlerType.BLOCKING));

        // Find all health groups
        Set<String> healthGroups = new HashSet<>();
        // with simple @HealthGroup annotations
        for (AnnotationInstance healthGroupAnnotation : index.getAnnotations(HEALTH_GROUP)) {
            healthGroups.add(healthGroupAnnotation.value().asString());
        }
        // with @HealthGroups repeatable annotations
        for (AnnotationInstance healthGroupsAnnotation : index.getAnnotations(HEALTH_GROUPS)) {
            for (AnnotationInstance healthGroupAnnotation : healthGroupsAnnotation.value().asNestedArray()) {
                healthGroups.add(healthGroupAnnotation.value().asString());
            }
        }

        // Register the health group handlers
        routes.produce(
                new RouteBuildItem(health.rootPath + health.groupPath, new SmallRyeHealthGroupHandler(),
                        HandlerType.BLOCKING));

        SmallRyeIndividualHealthGroupHandler handler = new SmallRyeIndividualHealthGroupHandler();
        for (String healthGroup : healthGroups) {
            routes.produce(
                    new RouteBuildItem(health.rootPath + health.groupPath + "/" + healthGroup,
                            handler, HandlerType.BLOCKING));
        }
    }

    @BuildStep
    public void kubernetes(HttpBuildTimeConfig httpConfig,
            BuildProducer<KubernetesHealthLivenessPathBuildItem> livenessPathItemProducer,
            BuildProducer<KubernetesHealthReadinessPathBuildItem> readinessPathItemProducer) {
        if (httpConfig.rootPath == null) {
            livenessPathItemProducer.produce(new KubernetesHealthLivenessPathBuildItem(health.rootPath + health.livenessPath));
            readinessPathItemProducer
                    .produce(new KubernetesHealthReadinessPathBuildItem(health.rootPath + health.readinessPath));
        } else {
            String basePath = httpConfig.rootPath.replaceAll("/$", "") + health.rootPath;
            livenessPathItemProducer.produce(new KubernetesHealthLivenessPathBuildItem(basePath + health.livenessPath));
            readinessPathItemProducer.produce(new KubernetesHealthReadinessPathBuildItem(basePath + health.readinessPath));
        }
    }

    @BuildStep
    ShutdownListenerBuildItem shutdownListener() {
        return new ShutdownListenerBuildItem(new ShutdownReadinessListener());
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer(BeanArchiveIndexBuildItem beanArchiveIndex,
            CustomScopeAnnotationsBuildItem scopes) {
        // Transform health checks that are not annotated with a scope or a stereotype
        Set<DotName> stereotypes = beanArchiveIndex.getIndex().getAnnotations(DotNames.STEREOTYPE).stream()
                .map(AnnotationInstance::name).collect(Collectors.toSet());
        List<DotName> healthAnnotations = new ArrayList<>(5);
        healthAnnotations.add(HEALTH);
        healthAnnotations.add(LIVENESS);
        healthAnnotations.add(READINESS);
        healthAnnotations.add(HEALTH_GROUP);
        healthAnnotations.add(HEALTH_GROUPS);

        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS || kind == Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.getAnnotations().isEmpty()) {
                    return;
                }
                Collection<AnnotationInstance> annotations;
                if (ctx.isClass()) {
                    annotations = ctx.getAnnotations();
                    if (containsAny(annotations, stereotypes)) {
                        return;
                    }
                } else {
                    annotations = getAnnotations(Kind.METHOD, ctx.getAnnotations());
                }
                if (scopes.isScopeIn(annotations)) {
                    return;
                }
                if (containsAny(annotations, healthAnnotations)) {
                    ctx.transform().add(BuiltinScope.SINGLETON.getName()).done();
                }
            }

        });
    }
}
