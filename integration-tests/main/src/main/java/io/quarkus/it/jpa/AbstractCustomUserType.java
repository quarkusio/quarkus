package io.quarkus.it.jpa;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.usertype.UserType;

public abstract class AbstractCustomUserType<T> implements UserType<T> {
    private final Class<T> clazz;

    protected AbstractCustomUserType(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> returnedClass() {
        return clazz;
    }

    @Override
    public boolean equals(T o, T o1) {
        return Objects.equals(o, o1);
    }

    @Override
    public int hashCode(T o) {
        return Objects.hashCode(o);
    }

    @Override
    public T deepCopy(T o) {
        return o;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(T o) {
        return (Serializable) o;
    }

    @Override
    public T assemble(Serializable cached, Object o) {
        return clazz.cast(cached);
    }

    @Override
    public T replace(T o, T t, Object owner) {
        return o;
    }
}
