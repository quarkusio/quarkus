package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/// Uses `@JsonNaming` with `UPPER_SNAKE_CASE` (deliberately different from the global
/// `SNAKE_CASE`) to test that class-level naming annotations are respected independently.
@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
public record AnnotationNamingRequest(String firstName) {
}
