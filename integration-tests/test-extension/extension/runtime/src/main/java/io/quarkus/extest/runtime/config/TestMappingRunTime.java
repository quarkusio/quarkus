package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.mapping.rt")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TestMappingRunTime {
    /**
     * A String value.
     */
    String value();

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
