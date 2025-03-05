package io.quarkus.extest.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.mapping.btrt")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface TestMappingBuildTimeRunTime {
    /**
     * A String value
     */
    String value();

    /**
     * A expression value
     */
    @WithDefault("${quarkus.mapping.btrt.expression.value}")
    Optional<String> expression();

    /**
     * A nested Group.
     */
    Group group();

    interface Group {
        /**
         * A Group value.
         */
        String value();
    }
}
