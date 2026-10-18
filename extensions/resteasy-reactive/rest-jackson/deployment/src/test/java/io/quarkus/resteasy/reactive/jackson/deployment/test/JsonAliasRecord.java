package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonAlias;

public record JsonAliasRecord(String name, @JsonAlias("insurance_number") String insuranceNumber) {
}
