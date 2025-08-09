package io.quarkus.it.panache.reactive;

import io.quarkus.hibernate.reactive.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CatDoubleConstructorWithProjectFieldNameAnnotation {

    public String name;

    public String ownerName;

    @SuppressWarnings("unused")
    public CatDoubleConstructorWithProjectFieldNameAnnotation(String name, String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
    }

    @SuppressWarnings("unused")
    public CatDoubleConstructorWithProjectFieldNameAnnotation(@ProjectedFieldName("name") String name) {
        this(name, null);
    }
}
