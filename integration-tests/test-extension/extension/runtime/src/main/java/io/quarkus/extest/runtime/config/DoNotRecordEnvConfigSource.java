package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.EnvConfigSource;

@StaticInitSafe
public class DoNotRecordEnvConfigSource extends EnvConfigSource {
    public DoNotRecordEnvConfigSource() {
        super(Map.of(
                "SHOULD_NOT_BE_RECORDED", "value",
                "should.not.be.recorded", "value",
                "quarkus.rt.do-not-record", "value",
                "quarkus.mapping.rt.do-not-record", "value",
                "%dev.quarkus.mapping.rt.do-not-record", "dev",
                "_PROD_QUARKUS_MAPPING_RT_DO_NOT_RECORD", "prod"), 300);
    }
}
