package io.quarkus.jaxb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "jaxb", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JaxbConfig {

    /**
     * Add other paths of jaxb.index
     */
    @ConfigItem
    public Optional<List<String>> indexPath;
}
