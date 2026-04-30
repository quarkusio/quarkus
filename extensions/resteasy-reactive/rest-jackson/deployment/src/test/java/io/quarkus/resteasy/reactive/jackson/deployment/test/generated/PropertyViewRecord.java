package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

public record PropertyViewRecord(
        @JsonView(GeneratedViews.Public.class) @JsonProperty("display_name") String name,
        @JsonView(GeneratedViews.Private.class) @JsonProperty("secret_code") String code,
        @JsonProperty("category") String category) {
}
