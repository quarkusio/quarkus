package io.quarkus.it.jackson.model;

import java.util.Collection;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record MammalFamily(Collection<Mammal> mammals) {
}
