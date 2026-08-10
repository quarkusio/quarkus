package io.quarkus.produi.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.prod-ui")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ProdUIBuildTimeConfig {

    /**
     * Enable Prod UI. When enabled, selected extension pages and JsonRPC methods
     * are available in production via the management interface.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Serve Prod UI on the management interface when the management interface is enabled.
     */
    @WithDefault("true")
    boolean managementEnabled();

    /**
     * URL path for Prod UI under the non-application root path.
     */
    @WithDefault("prod-ui")
    String path();
}
