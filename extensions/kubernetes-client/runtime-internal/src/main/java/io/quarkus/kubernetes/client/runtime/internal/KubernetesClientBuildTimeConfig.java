package io.quarkus.kubernetes.client.runtime.internal;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.kubernetes-client")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KubernetesClientBuildTimeConfig {

    /**
     * Enable the generation of the RBAC manifests. If enabled and no other role binding are provided using the properties
     * `quarkus.kubernetes.rbac.`, it will generate a default role binding using the role "view" and the application
     * service account.
     */
    @WithDefault("true")
    boolean generateRbac();

    /**
     * Dev Services
     */
    @ConfigDocSection(generated = true)
    KubernetesDevServicesBuildTimeConfig devservices();
}
