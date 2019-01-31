package org.jboss.shamrock.deployment;

import java.util.Optional;

import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.SslNativeConfigBuildItem;
import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

public class SslProcessor {

    Config ssl;

    @ConfigGroup
    static class Config {
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
