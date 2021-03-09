package io.quarkus.resteasy.reactive.client.microprofile.recorder;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.resteasy.reactive.client.microprofile.BuilderResolver;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }
}
