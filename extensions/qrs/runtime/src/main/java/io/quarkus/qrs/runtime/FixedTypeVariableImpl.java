package io.quarkus.qrs.runtime;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;

import io.quarkus.arc.impl.TypeVariableImpl;

// FIXME: move to ArC
public class FixedTypeVariableImpl<D extends GenericDeclaration> extends TypeVariableImpl<D> {

    public FixedTypeVariableImpl(String name, Type... bounds) {
        super(name, bounds);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return getName().hashCode()
                ^ Arrays.hashCode(getBounds());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof TypeVariable) {
            TypeVariable<?> that = (TypeVariable<?>) obj;
            return Objects.equals(getName(), that.getName())
                    && Arrays.equals(getBounds(), that.getBounds());
        } else {
            return false;
        }
    }
}
