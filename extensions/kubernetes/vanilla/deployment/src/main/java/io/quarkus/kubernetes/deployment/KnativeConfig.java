package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class KnativeConfig extends KubernetesCommonConfig {

    /**
     * Whether this service is cluster-local.
     * Cluster local services are not exposed to the outside world.
     * More information in <a href="https://knative.dev/docs/serving/services/private-services/">this link</a>.
     */
    @ConfigItem
    public boolean clusterLocal;

    /**
     * This value controls the minimum number of replicas each revision should have.
     * Knative will attempt to never have less than this number of replicas at any point in time.
     */
    @ConfigItem
    Optional<Integer> minScale;

    /**
     * This value controls the maximum number of replicas each revision should have.
     * Knative will attempt to never have more than this number of replicas running, or in the process of being created, at any
     * point in time.
     **/
    @ConfigItem
    Optional<Integer> maxScale;

    /**
     * The scale-to-zero values control whether Knative allows revisions to scale down to zero, or stops at “1”.
     */
    @ConfigItem(defaultValue = "true")
    boolean scaleToZeroEnabled;

    /**
     * Revision autoscaling configuration.
     */
    AutoScalingConfig revisionAutoScaling;

    /**
     * Global autoscaling configuration.
     */
    GlobalAutoScalingConfig globalAutoScaling;

    /**
     * The name of the revision.
     */
    @ConfigItem
    Optional<String> revisionName;

    /**
     * Traffic configuration.
     */
    @ConfigItem
    Map<String, TrafficConfig> traffic;

    /**
     * If set to true, Quarkus will attempt to deploy the application to the target knative cluster
     */
    @ConfigItem(defaultValue = "false")
    boolean deploy;

    /**
     * If deploy is enabled, it will follow this strategy to update the resources to the target Knative cluster.
     */
    @ConfigItem(defaultValue = "CreateOrUpdate")
    DeployStrategy deployStrategy;

    @Override
    public String getTargetPlatformName() {
        return Constants.KNATIVE;
    }
}
