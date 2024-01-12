package io.quarkus.it.mongodb.panache.record;

import io.quarkus.mongodb.panache.common.ProjectionFor;

@ProjectionFor(PersonRecord.class)
public record PersonName(String firstName, String lastName) {
}
