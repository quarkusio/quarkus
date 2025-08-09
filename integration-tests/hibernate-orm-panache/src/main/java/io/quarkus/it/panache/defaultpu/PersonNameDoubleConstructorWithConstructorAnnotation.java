package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.common.ProjectedConstructor;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PersonNameDoubleConstructorWithConstructorAnnotation extends PersonName {
    @SuppressWarnings("unused")
    public PersonNameDoubleConstructorWithConstructorAnnotation(String uniqueName, String name, Object fakeParameter) {
        super(uniqueName, name);
    }

    @ProjectedConstructor
    @SuppressWarnings("unused")
    public PersonNameDoubleConstructorWithConstructorAnnotation(String uniqueName, String name) {
        super(uniqueName, name);
    }
}