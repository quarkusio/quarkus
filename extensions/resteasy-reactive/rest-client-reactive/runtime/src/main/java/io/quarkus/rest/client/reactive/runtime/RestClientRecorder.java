package io.quarkus.rest.client.reactive.runtime;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }
}
