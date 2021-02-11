
package io.quarkus.kubernetes.deployment;

public enum AutoScalingMetric {
    /**
     * Concurrency
     */
    concurrency,

    /**
     * Requests per second
     **/
    rps,

    /**
     * CPU
     **/
    cpu;
}
