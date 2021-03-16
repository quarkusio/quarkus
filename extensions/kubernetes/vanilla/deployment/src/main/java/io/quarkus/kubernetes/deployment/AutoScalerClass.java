
package io.quarkus.kubernetes.deployment;

public enum AutoScalerClass {

    /**
     * Kubernetes Pod Autoscaler
     **/
    kpa,

    /**
     * Horizontal Pod Autoscaler
     **/
    hpa
}
