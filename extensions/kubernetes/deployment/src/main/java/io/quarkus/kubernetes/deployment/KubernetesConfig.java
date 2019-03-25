package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class KubernetesConfig {

    /**
     * Controls whether or not the generated Kubernetes resources should be applied to the cluster or not
     */
    public boolean deploy = false;

    /**
     * Configuration that is relevant to docker images
     */
    public DockerConfig docker;
}
