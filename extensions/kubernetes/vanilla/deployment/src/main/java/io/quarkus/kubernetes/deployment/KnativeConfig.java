package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Knative
 */
@ConfigMapping(prefix = "quarkus.knative")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KnativeConfig extends PlatformConfiguration {

    @Override
    default String targetPlatformName() {
        return Constants.KNATIVE;
    }

    /**
     * Whether this service is cluster-local.
     * Cluster local services are not exposed to the outside world.
     * More information in <a href="https://knative.dev/docs/serving/services/private-services/">this link</a>.
     */
    @WithDefault("false")
    boolean clusterLocal();

    /**
     * This value controls the minimum number of replicas each revision should have.
     * Knative will attempt to never have less than this number of replicas at any point in time.
     */
    Optional<Integer> minScale();

    /**
     * This value controls the maximum number of replicas each revision should have.
     * Knative will attempt to never have more than this number of replicas running, or in the process of being created, at any
     * point in time.
     **/
    Optional<Integer> maxScale();

    /**
     * The scale-to-zero values control whether Knative allows revisions to scale down to zero, or stops at “1”.
     */
    @WithDefault("true")
    boolean scaleToZeroEnabled();

    /**
     * Revision autoscaling configuration.
     */
    AutoScalingConfig revisionAutoScaling();

    /**
     * Global autoscaling configuration.
     */
    GlobalAutoScalingConfig globalAutoScaling();

    /**
     * The name of the revision.
     */
    Optional<String> revisionName();

    /**
     * Traffic configuration.
     */
    Map<String, TrafficConfig> traffic();

    /**
     * If set to true, Quarkus will attempt to deploy the application to the target Kubernetes cluster
     */
    @WithDefault("false")
    boolean deploy();

    /**
     * If deploy is enabled, it will follow this strategy to update the resources to the target Kubernetes cluster.
     */
    @WithDefault("CreateOrUpdate")
    DeployStrategy deployStrategy();
}
