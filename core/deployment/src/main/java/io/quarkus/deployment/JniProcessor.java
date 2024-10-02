package io.quarkus.deployment;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class JniProcessor {

    private static final Logger LOGGER = Logger.getLogger(JniProcessor.class);

    JniConfig jni;

    /**
     * JNI
     */
    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static class JniConfig {
        /**
         * Paths of library to load.
         */
        @ConfigItem
        Optional<List<String>> libraryPaths;

        /**
         * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
         */
        @Deprecated
        @ConfigItem(defaultValue = "true")
        boolean enable = true;
    }

    @BuildStep
    void setupJni(BuildProducer<JniBuildItem> jniProducer) {
        if (!jni.enable) {
            LOGGER.warn("Your application is setting the deprecated 'quarkus.jni.enable' configuration key to false. Please"
                    + " consider removing this configuration key as it is ignored (JNI is always enabled) and it will be"
                    + " removed in a future Quarkus version.");
        }
        if (jni.libraryPaths.isPresent()) {
            jniProducer.produce(new JniBuildItem(jni.libraryPaths.get()));
        }
    }
}
