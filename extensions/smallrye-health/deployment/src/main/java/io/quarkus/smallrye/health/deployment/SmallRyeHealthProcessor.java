package io.quarkus.smallrye.health.deployment;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
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
    void build(SmallRyeHealthRecorder recorder, RecorderContext recorderContext,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            LaunchModeBuildItem launchModeBuildItem) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_HEALTH));

        // add health endpoints to not found page
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.livenessPath));
            displayableEndpoints
                    .produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.readinessPath));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(health.rootPath + health.groupPath));
        }

        // Make ArC discover the beans marked with the @Health qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH));

        // Make ArC discover the beans marked with the @Liveness qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(LIVENESS));

        // Make ArC discover the beans marked with the @Readiness qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(READINESS));

        // Make ArC discover the beans marked with the @HealthGroup qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH_GROUP));

        // Make ArC discover the beans marked with the repeatable @HealthGroups annotation
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

        recorder.registerHealthCheckResponseProvider(
                (Class<? extends HealthCheckResponseProvider>) recorderContext.classProxy(providers.iterator().next()));
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
}
