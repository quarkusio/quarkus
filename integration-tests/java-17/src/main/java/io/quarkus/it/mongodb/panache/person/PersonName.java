package io.quarkus.it.mongodb.panache.person;

import io.quarkus.mongodb.panache.common.ProjectionFor;

@ProjectionFor(Person.class)
public record PersonName(String firstName, String lastName) {
}
