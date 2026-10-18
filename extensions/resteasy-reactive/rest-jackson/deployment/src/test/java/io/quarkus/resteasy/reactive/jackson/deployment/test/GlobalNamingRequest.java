package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

/// A simple record with camelCase field names and NO {@code @JsonNaming} annotation.
/// Relies solely on the global {@code PropertyNamingStrategy} configured on the ObjectMapper.
/// The {@code yearsOld} field has an explicit {@code @JsonProperty("age")} which must
/// take precedence over the global strategy (i.e. it must NOT be translated to {@code years_old}
/// by the strategy — it already is {@code age} via the annotation).
public record GlobalNamingRequest(String firstName, String lastName, @JsonProperty("age") int yearsOld) {
}
