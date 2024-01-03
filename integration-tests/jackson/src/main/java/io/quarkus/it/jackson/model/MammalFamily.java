package io.quarkus.it.jackson.model;

import java.util.Collection;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MammalFamily {

    private final Collection<Mammal> mammals;

    public MammalFamily(Collection<Mammal> mammals) {
        this.mammals = mammals;
    }

    public Collection<Mammal> getMammals() {
        return mammals;
    }
}
