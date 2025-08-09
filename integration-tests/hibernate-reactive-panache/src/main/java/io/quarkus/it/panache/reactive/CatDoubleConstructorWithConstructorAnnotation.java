package io.quarkus.it.panache.reactive;

import io.quarkus.hibernate.reactive.panache.common.ProjectedConstructor;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CatDoubleConstructorWithConstructorAnnotation {

    public String name;

    public String ownerName;

    @SuppressWarnings("unused")
    public CatDoubleConstructorWithConstructorAnnotation(String name, String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
    }

    @ProjectedConstructor
    @SuppressWarnings("unused")
    public CatDoubleConstructorWithConstructorAnnotation(String name) {
        this(name, null);
    }
}
