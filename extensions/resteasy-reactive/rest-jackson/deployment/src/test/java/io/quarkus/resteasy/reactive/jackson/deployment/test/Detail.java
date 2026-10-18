package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Detail(@JsonProperty("id") String id, @JsonProperty("value") String value) {
}
