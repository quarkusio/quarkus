package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

public class SslProcessor {

    private static final String JAVA_11_PLUS_SSL_LOGGER = "sun.security.ssl.SSLLogger";

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

    @BuildStep
    void runtime(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        reinitialized.produce(new RuntimeReinitializedClassBuildItem(JAVA_11_PLUS_SSL_LOGGER));
    }
}
