package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.mapping.btrt")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface TestMappingBuildTimeRunTime {
    /**
     * A String value.
     */
    String value();

    /**
     * A nested Group.
     */
    Group group();

    /**
     * A Map of Map.
     */
    Map<String, Map<String, String>> mapMap();

    interface Group {
        /**
         * A Group value.
         */
        String value();
    }
}
