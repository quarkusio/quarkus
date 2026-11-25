package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

public class SslProcessor {

    SslConfig ssl;

    /**
     * SSL
     */
    @ConfigMapping(prefix = "quarkus.ssl")
    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    interface SslConfig {
        /**
         * Enable native SSL support.
         */
        @WithName("native")
        Optional<Boolean> native_();
    }

    @BuildStep
    SslNativeConfigBuildItem setupNativeSsl() {
        return new SslNativeConfigBuildItem(ssl.native_());
    }
}
