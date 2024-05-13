package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.smallrye.config.EnvConfigSource;

public class EnvBuildTimeConfigSource extends EnvConfigSource {
    public EnvBuildTimeConfigSource() {
        super(Map.of(
                "QUARKUS_PROFILE", "record",
                "QUARKUS_MAPPING_RT_DO_NOT_RECORD", "value",
                "BT_OK_TO_RECORD", "from-env",
                "BT_DO_NOT_RECORD", "value",
                "DO_NOT_RECORD", "value"), Integer.MAX_VALUE);
    }
}
