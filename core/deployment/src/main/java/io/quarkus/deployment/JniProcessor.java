package io.quarkus.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

public class JniProcessor {
    JniConfig jni;

    /**
     * JNI
     */
    @ConfigMapping(prefix = "quarkus.jni")
    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    interface JniConfig {
        /**
         * Paths of library to load.
         */
        Optional<List<String>> libraryPaths();
    }

    @BuildStep
    void setupJni(BuildProducer<JniBuildItem> jniProducer) {
        if (jni.libraryPaths().isPresent()) {
            jniProducer.produce(new JniBuildItem(jni.libraryPaths().get()));
        }
    }
}
