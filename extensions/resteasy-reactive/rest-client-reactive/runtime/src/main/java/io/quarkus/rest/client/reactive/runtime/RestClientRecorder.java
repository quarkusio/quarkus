package io.quarkus.rest.client.reactive.runtime;

import java.util.Map;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    private static volatile Map<String, String> configKeys;

    public void setConfigKeys(Map<String, String> configKeys) {
        RestClientRecorder.configKeys = configKeys;
    }

    public static Map<String, String> getConfigKeys() {
        return configKeys;
    }

    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }
}
