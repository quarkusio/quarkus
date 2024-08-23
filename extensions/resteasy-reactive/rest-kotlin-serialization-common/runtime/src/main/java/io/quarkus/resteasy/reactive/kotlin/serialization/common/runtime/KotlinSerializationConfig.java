package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime;

import java.util.StringJoiner;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = KotlinSerializationConfig.CONFIG_NAME, phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class KotlinSerializationConfig {
    public static final String CONFIG_NAME = "kotlin-serialization";

    /**
     * Configuration element for serializing to json
     */
    @ConfigItem(name = "json")
    public JsonConfig json = new JsonConfig();

    @Override
    public String toString() {
        return new StringJoiner(", ", KotlinSerializationConfig.class.getSimpleName() + "[", "]")
                .add("json=" + json)
                .toString();
    }
}
