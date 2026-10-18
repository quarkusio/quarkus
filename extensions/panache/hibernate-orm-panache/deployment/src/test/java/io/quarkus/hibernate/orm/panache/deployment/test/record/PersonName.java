package io.quarkus.hibernate.orm.panache.deployment.test.record;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PersonName(String firstname, String lastname) {
}
