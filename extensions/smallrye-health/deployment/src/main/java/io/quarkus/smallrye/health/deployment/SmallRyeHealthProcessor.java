package io.quarkus.smallrye.health.deployment;

import java.io.IOException;
import java.util.Set;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRecorder;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthServlet;
import io.quarkus.smallrye.health.runtime.SmallRyeLivenessServlet;
import io.quarkus.smallrye.health.runtime.SmallRyeReadinessServlet;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.smallrye.health.SmallRyeHealthReporter;

class SmallRyeHealthProcessor {

    private static final DotName HEALTH = DotName.createSimple(Health.class.getName());

    private static final DotName LIVENESS = DotName.createSimple(Liveness.class.getName());

    private static final DotName READINESS = DotName.createSimple(Readiness.class.getName());

    /**
     * The configuration for health checking.
     */
    SmallRyeHealthConfig health;

    @ConfigRoot(name = "smallrye-health")
    static final class SmallRyeHealthConfig {
        /**
         * Root path for health-checking servlets.
         */
        @ConfigItem(defaultValue = "/health")
        String rootPath;

        /**
         * The relative path of the liveness health-checking servlet.
         */
        @ConfigItem(defaultValue = "/live")
        String livenessPath;

        /**
         * The relative path of the readiness health-checking servlet.
         */
        @ConfigItem(defaultValue = "/ready")
        String readinessPath;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    void build(SmallRyeHealthRecorder recorder, RecorderContext recorderContext,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_HEALTH));

        // Register the health servlet
        ServletBuildItem servletBuildItem = ServletBuildItem.builder("health", SmallRyeHealthServlet.class.getName())
                .addMapping(health.rootPath).build();
        servlet.produce(servletBuildItem);

        // Register the liveness servlet
        ServletBuildItem liveServletBuildItem = ServletBuildItem.builder("liveness", SmallRyeLivenessServlet.class.getName())
                .addMapping(health.rootPath + health.livenessPath).build();
        servlet.produce(liveServletBuildItem);

        // Register the readiness servlet
        ServletBuildItem readyServletBuildItem = ServletBuildItem.builder("readiness", SmallRyeReadinessServlet.class.getName())
                .addMapping(health.rootPath + health.readinessPath).build();
        servlet.produce(readyServletBuildItem);

        // Make ArC discover the beans marked with the @Health qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(HEALTH));

        // Make ArC discover the beans marked with the @Liveness qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(LIVENESS));

        // Make ArC discover the beans marked with the @Readiness qualifier
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(READINESS));

        // Add additional beans
        additionalBean.produce(new AdditionalBeanBuildItem(SmallRyeHealthReporter.class,
                SmallRyeHealthServlet.class,
                SmallRyeLivenessServlet.class,
                SmallRyeReadinessServlet.class));

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
    public void kubernetes(BuildProducer<KubernetesHealthLivenessPathBuildItem> livenessPathItemProducer,
            BuildProducer<KubernetesHealthReadinessPathBuildItem> readinessPathItemProducer) {
        livenessPathItemProducer
                .produce(new KubernetesHealthLivenessPathBuildItem(health.rootPath + health.livenessPath));
        readinessPathItemProducer
                .produce(new KubernetesHealthReadinessPathBuildItem(health.rootPath + health.readinessPath));
    }
}
