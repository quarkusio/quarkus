package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "mm-root")
public class MapMapConfig {
    //### map of map of strings
    //quarkus.mm-root.map.inner-key.outer-key=1234

    Map<String, Map<String, String>> map;
}
