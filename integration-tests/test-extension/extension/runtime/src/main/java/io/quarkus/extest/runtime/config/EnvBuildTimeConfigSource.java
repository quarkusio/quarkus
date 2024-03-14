package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.smallrye.config.EnvConfigSource;

public class EnvBuildTimeConfigSource extends EnvConfigSource {
    public EnvBuildTimeConfigSource() {
        super(Map.of("QUARKUS_RT_RT_STRING_OPT", "changed",
                "BT_OK_TO_RECORD", "env-source",
                "DO_NOT_RECORD", "record"), Integer.MAX_VALUE);
    }
}
