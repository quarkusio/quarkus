package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PolymorphicItemResponse(@JsonProperty("item") PolymorphicItem item) {
}
