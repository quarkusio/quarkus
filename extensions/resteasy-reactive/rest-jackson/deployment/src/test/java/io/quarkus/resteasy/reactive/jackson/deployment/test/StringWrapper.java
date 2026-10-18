package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.validation.constraints.NotNull;

public record StringWrapper(@NotNull String text) {
}
