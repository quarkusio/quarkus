package io.quarkus.extest.runtime.config;

import java.util.HashMap;

import io.smallrye.config.EnvConfigSource;

public class EnvBuildTimeConfigSource extends EnvConfigSource {
    public EnvBuildTimeConfigSource() {
        super(new HashMap<>() {
            {
                put("QUARKUS_RT_RT_STRING_OPT", "changed");
                put("BT_DO_NOT_RECORD", "env-source");
            }
        }, Integer.MAX_VALUE);
    }
}
