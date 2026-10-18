package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;

/**
 * Structure storing details of method parameters.
 */
public class TypeInfo {

    private Class<?> name;

    private List<Class<?>> generics;

    public Class<?> getName() {
        return name;
    }

    public void setName(Class<?> name) {
        this.name = name;
    }

    public List<Class<?>> getGenerics() {
        return generics;
    }

    public void setGenerics(List<Class<?>> generics) {
        this.generics = generics;
    }
}
