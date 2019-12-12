package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class KubernetesConfig {

    /**
     * Skip apply resources to cluster
     */
    @ConfigItem(defaultValue = "false")
    public boolean skipApply;
}
