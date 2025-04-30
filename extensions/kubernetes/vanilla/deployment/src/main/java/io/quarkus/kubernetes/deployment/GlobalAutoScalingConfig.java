package io.quarkus.kubernetes.deployment;

import java.util.Optional;

public interface GlobalAutoScalingConfig {
    /**
     * The Autoscaler class. Knative Serving comes with its own autoscaler, the KPA (Knative Pod Autoscaler) but can
     * also be configured to use Kubernetes’ HPA (Horizontal Pod Autoscaler) or even a custom third-party autoscaler.
     * Possible values (kpa, hpa, default: kpa).
     */
    Optional<AutoScalerClass> autoScalerClass();

    /**
     * The exact amount of requests allowed to the replica at a time. Its default value is “0”, which means an
     * unlimited number of requests are allowed to flow Integer>o the replica.
     *
     * @see <a href="https://knative.dev/docs/serving/autoscaling/concurrency/#hard-limit">Knative Knative: Configuring
     *      concurrency: Hard Limit</a>
     */
    Optional<Integer> containerConcurrency();

    /**
     * This value specifies a percentage of the target to actually be targeted by the autoscaler.
     */
    Optional<Integer> targetUtilizationPercentage();

    /**
     * The requests per second per replica.
     */
    Optional<Integer> requestsPerSecond();
}
