package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME, name = "test")
public class KubernetesConfig {

    /**
     * Skip image build
     */
    @ConfigItem(defaultValue = "false")
    public boolean skipBuild;

    /**
     * Skip apply resources to cluster
     */
    @ConfigItem(defaultValue = "false")
    public boolean skipApply;
}
