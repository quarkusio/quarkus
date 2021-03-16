package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class SslProcessor {

    private static final String JAVA_11_PLUS_SSL_LOGGER = "sun.security.ssl.SSLLogger";
    private static final String JAVA_8_PLUS_SSL_LOGGER = "sun.security.ssl.Debug";

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

    @BuildStep
    void runtime(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        registerIfExists(reinitialized, JAVA_11_PLUS_SSL_LOGGER);
        registerIfExists(reinitialized, JAVA_8_PLUS_SSL_LOGGER);
    }

    private void registerIfExists(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized, String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            reinitialized.produce(new RuntimeReinitializedClassBuildItem(className));
        } catch (ClassNotFoundException ignored) {

        }
    }
}
