package io.quarkus.deployment.execannotations;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.execution-model-annotations")
public interface ExecutionModelAnnotationsConfig {
    /**
     * Detection mode of invalid usage of execution model annotations.
     * <p>
     * An execution model annotation is {@code @Blocking}, {@code @NonBlocking} and {@code @RunOnVirtualThread}.
     * These annotations may only be used on "entrypoint" methods (methods invoked by various frameworks in Quarkus);
     * using them on methods that can only be invoked by application code is invalid.
     */
    @WithDefault("fail")
    Mode detectionMode();

    enum Mode {
        /**
         * Invalid usage of execution model annotations causes build failure.
         */
        FAIL,
        /**
         * Invalid usage of execution model annotations causes warning during build.
         */
        WARN,
        /**
         * No detection of invalid usage of execution model annotations.
         */
        DISABLED,
    }
}
