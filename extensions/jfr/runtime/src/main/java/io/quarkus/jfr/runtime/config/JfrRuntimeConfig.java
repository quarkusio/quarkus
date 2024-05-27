package io.quarkus.jfr.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.jfr")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JfrRuntimeConfig {

    /**
     * If false, only quarkus-jfr events are not recorded even if JFR is enabled.
     * In this case, Java standard API and virtual machine information will be recorded according to the setting.
     * Default value is <code>true</code>
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * If false, only REST events in quarkus-jfr are not recorded even if JFR is enabled.
     * In this case, other quarkus-jfr, Java standard API and virtual machine information will be recorded according to the
     * setting.
     * Default value is <code>true</code>
     */
    @WithName("rest.enabled")
    @WithDefault("true")
    boolean restEnabled();
}
