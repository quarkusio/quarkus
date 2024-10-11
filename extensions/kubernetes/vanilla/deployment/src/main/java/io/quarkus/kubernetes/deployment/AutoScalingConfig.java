package io.quarkus.kubernetes.deployment;

import java.util.Optional;

public interface AutoScalingConfig {
    /**
     * The Autoscaler class. Knative Serving comes with its own autoscaler, the KPA (Knative Pod Autoscaler) but can
     * also be configured to use Kubernetes’ HPA (Horizontal Pod Autoscaler) or even a custom third-party autoscaler.
     * Possible values (kpa, hpa, default: kpa).
     */
    Optional<AutoScalerClass> autoScalerClass();

    /**
     * The autoscaling metric to use. Possible values (concurrency, rps, cpu).
     */
    Optional<AutoScalingMetric> metric();

    /**
     * The autoscaling target.
     */
    Optional<Integer> target();

    /**
     * The exact amount of requests allowed to the replica at a time. Its default value is “0”, which means an
     * unlimited number of requests are allowed to flow into the replica.
     */
    Optional<Integer> containerConcurrency();

    /**
     * This value specifies a percentage of the target to actually be targeted by the autoscaler.
     */
    Optional<Integer> targetUtilizationPercentage();
}
