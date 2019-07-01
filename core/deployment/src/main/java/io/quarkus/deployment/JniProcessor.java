package io.quarkus.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class JniProcessor {

    JniConfig jni;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static class JniConfig {
        /**
         * Paths of library to load.
         */
        @ConfigItem
        List<String> libraryPaths;

        /**
         * Enable JNI support.
         */
        @ConfigItem(defaultValue = "false")
        boolean enable = false;
    }

    @BuildStep
    void setupJni(BuildProducer<JniBuildItem> jniProducer) {
        if ((jni.enable) || !jni.libraryPaths.isEmpty()) {
            jniProducer.produce(new JniBuildItem(jni.libraryPaths));
        }
    }
}
