package io.quarkus.http3.deployment;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.http3.deployment.spi.Http3EnabledBuildItem;
import io.quarkus.http3.runtime.Http3Customizer;
import io.quarkus.runtime.configuration.ConfigurationException;

class Http3Processor {

    private static final Logger LOG = Logger.getLogger(Http3Processor.class);

    private static final String FEATURE = "http3";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    Http3EnabledBuildItem enable(Http3BuildTimeConfig config) {
        if (!config.enabled()) {
            return null;
        }

        if (!isQuicNativeAvailable()) {
            throw new ConfigurationException(
                    "HTTP/3 is enabled but the native QUIC library is not on the classpath. " +
                            "Add a dependency on io.netty:netty-codec-native-quic with the classifier " +
                            "matching your platform (e.g. linux-x86_64, osx-aarch_64). " +
                            "Set quarkus.http3.enabled=false to disable HTTP/3.");
        }

        LOG.info("HTTP/3 (QUIC) support enabled");
        return new Http3EnabledBuildItem();
    }

    @BuildStep
    AdditionalBeanBuildItem registerCustomizer() {
        return AdditionalBeanBuildItem.unremovableOf(Http3Customizer.class);
    }

    private static boolean isQuicNativeAvailable() {
        return QuarkusClassLoader.isClassPresentAtRuntime("io.netty.handler.codec.quic.Quiche");
    }

}
