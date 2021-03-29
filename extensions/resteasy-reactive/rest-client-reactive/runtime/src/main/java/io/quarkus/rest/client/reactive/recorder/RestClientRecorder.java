package io.quarkus.rest.client.reactive.recorder;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.rest.client.reactive.BuilderResolver;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }
}
