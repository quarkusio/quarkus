package io.quarkus.resteasy.reactive.jackson.deployment.test;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/// Uses `@JsonNaming` with `UPPER_SNAKE_CASE` (deliberately different from the global
/// `SNAKE_CASE`) to test that class-level naming annotations are respected independently.
@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
public record AnnotationNamingRequest(String firstName) {
}
