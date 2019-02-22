package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class SslProcessor {

    SslConfig ssl;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static class SslConfig {
        /**
         * Enable native SSL support.
         */
        @ConfigItem(name = "native")
        Optional<Boolean> native_;
    }

    @BuildStep
    SslNativeConfigBuildItem setupNativeSsl() {
        return new SslNativeConfigBuildItem(ssl.native_);
    }
}
