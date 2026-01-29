package io.quarkus.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

public class JniProcessor {
    JniConfig jni;

    /**
     * JNI
     *
     * @deprecated This configuration was previously used to enable JNI from Quarkus extensions,
     *             but JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Deprecated(since = "3.32", forRemoval = true)
    @ConfigMapping(prefix = "quarkus.jni")
    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    interface JniConfig {
        /**
         * Paths of library to load.
         */
        Optional<List<String>> libraryPaths();
    }
}
