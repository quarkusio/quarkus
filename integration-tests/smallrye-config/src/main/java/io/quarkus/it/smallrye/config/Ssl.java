package io.quarkus.it.smallrye.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.smallrye.config.WithDefault;

// TODO - Add hierarchy here but requires https://github.com/smallrye/smallrye-config/pull/590
public interface Ssl {
    @JsonProperty
    int port();

    @JsonProperty
    String certificate();

    @JsonProperty
    @WithDefault("TLSv1.3,TLSv1.2")
    List<String> protocols();
}
