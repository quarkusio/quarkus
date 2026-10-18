package io.quarkus.it.smallrye.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.config.WithDefault;

@RegisterForReflection
public interface Ssl {
    @JsonProperty
    int port();

    @JsonProperty
    String certificate();

    @JsonProperty
    @WithDefault("TLSv1.3,TLSv1.2")
    List<String> protocols();
}
