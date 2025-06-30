package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.extension-loader")
@ConfigRoot
public interface ExtensionLoaderConfig {
    /**
     * Report runtime Config objects used during deployment time.
     */
    @WithDefault("warn")
    ReportRuntimeConfigAtDeployment reportRuntimeConfigAtDeployment();

    enum ReportRuntimeConfigAtDeployment {
        warn,
        fail
    }
}
