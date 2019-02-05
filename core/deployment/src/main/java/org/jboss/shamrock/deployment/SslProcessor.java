package org.jboss.shamrock.deployment;

import java.util.Optional;

import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.SslNativeConfigBuildItem;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

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
