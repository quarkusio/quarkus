package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UnwrappedResultsResponse(@JsonProperty("results") List<UnwrappedResult> results) {
}
