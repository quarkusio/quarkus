package io.quarkus.jackson.runtime;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JacksonRecorder {

    public Supplier<JacksonConfigSupport> jacksonConfigSupport(JacksonBuildTimeConfig jacksonBuildTimeConfig) {
        return new Supplier<JacksonConfigSupport>() {

            @Override
            public JacksonConfigSupport get() {
                return new JacksonConfigSupport(jacksonBuildTimeConfig.failOnUnknownProperties,
                        jacksonBuildTimeConfig.failOnEmptyBeans,
                        jacksonBuildTimeConfig.writeDatesAsTimestamps,
                        jacksonBuildTimeConfig.acceptCaseInsensitiveEnums,
                        jacksonBuildTimeConfig.timezone.orElse(null),
                        jacksonBuildTimeConfig.serializationInclusion.orElse(null));
            }
        };
    }
}
