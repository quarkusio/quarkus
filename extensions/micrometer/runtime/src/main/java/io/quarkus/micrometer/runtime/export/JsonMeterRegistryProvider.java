package io.quarkus.micrometer.runtime.export;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Clock;
import io.quarkus.micrometer.runtime.registry.json.JsonMeterRegistry;

@Singleton
public class JsonMeterRegistryProvider {

    @Produces
    @Singleton
    public JsonMeterRegistry registry(Clock clock, io.quarkus.micrometer.runtime.config.MicrometerConfig config) {
        return new JsonMeterRegistry(clock, config.export.json.bufferLength, config.export.json.expiry);
    }
}
