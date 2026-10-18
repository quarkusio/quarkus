package io.quarkus.extest.runtime.classpath;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.bt.classpath-recording")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ClasspathRecordingConfig {
    /**
     * Names of resources for which classpath entries should be recorded.
     */
    Optional<List<String>> resources();

    /**
     * Path to the file to which classpath entry records will be appended.
     */
    Optional<Path> recordFile();
}
