package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.EnvConfigSource;

@StaticInitSafe
public class RecordQuarkusSystemPropertiesConfigSource extends EnvConfigSource {
    public RecordQuarkusSystemPropertiesConfigSource() {
        super(Map.of(
                "SHOULD_NOT_BE_RECORDED", "value",
                "should.not.be.recorded", "value",
                "quarkus.mapping.rt.record", "value",
                "%dev.quarkus.mapping.rt.record", "dev",
                "_PROD_QUARKUS_MAPPING_RT_RECORD", "prod"), 0);
    }
}
