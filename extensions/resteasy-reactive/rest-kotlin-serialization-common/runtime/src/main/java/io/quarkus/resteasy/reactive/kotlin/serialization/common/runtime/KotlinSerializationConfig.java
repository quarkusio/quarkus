package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = KotlinSerializationConfig.CONFIG_PREFIX)
public interface KotlinSerializationConfig {
    public static final String CONFIG_PREFIX = "quarkus.kotlin-serialization";

    /**
     * Configuration element for serializing to json
     */
    public JsonConfig json();
}
