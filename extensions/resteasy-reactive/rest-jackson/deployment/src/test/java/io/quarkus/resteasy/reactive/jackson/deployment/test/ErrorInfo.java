package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorInfo(@JsonProperty("code") String code, @JsonProperty("message") String message) {
}
