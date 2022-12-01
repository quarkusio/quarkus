package io.quarkus.it.hibernate.panache.person;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PersonName(String firstname, String lastname) {
}
