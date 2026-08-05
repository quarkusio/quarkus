package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({ "temp", "debug" })
public record IgnorePropertiesCreatorRecord(
        @JsonProperty("name") String name,
        @JsonProperty("value") int value) {
}
