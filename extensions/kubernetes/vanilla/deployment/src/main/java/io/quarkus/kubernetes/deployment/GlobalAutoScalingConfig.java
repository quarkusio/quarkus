package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public class GlobalAutoScalingConfig {

    /**
     * The Autoscaler class.
     * Knative Serving comes with its own autoscaler, the KPA (Knative Pod Autoscaler) but can also be configured to use
     * Kubernetes’ HPA (Horizontal Pod Autoscaler) or even a custom third-party autoscaler.
     * Possible values (kpa, hpa, default: kpa).
     * 
     * @return The autoscaler class.
     */
    Optional<AutoScalerClass> autoScalerClass;

    /**
     * The exact amount of requests allowed to the replica at a time.
     * Its default value is “0”, which means an unlimited number of requests are allowed to flow Integer>o the replica.
     * 
     * @return the container concurrenct or zero if its not bound.
     */
    Optional<Integer> containerConcurrency;

    /**
     * This value specifies a percentage of the target to actually be targeted by the autoscaler.
     */
    Optional<Integer> targetUtilizationPercentage;

    /**
     * The requests per second per replica.
     */
    Optional<Integer> requestsPerSecond;

}
