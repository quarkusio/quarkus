package io.quarkus.runtime.types;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * @author Marko Luksa
 * @author Jozef Hartinger
 */
public class GenericArrayTypeImpl implements GenericArrayType {

    private Type genericComponentType;

    public GenericArrayTypeImpl(Type genericComponentType) {
        this.genericComponentType = genericComponentType;
    }

    public GenericArrayTypeImpl(Class<?> rawType, Type... actualTypeArguments) {
        this.genericComponentType = new ParameterizedTypeImpl(rawType, actualTypeArguments);
    }

    @Override
    public Type getGenericComponentType() {
        return genericComponentType;
    }

    @Override
    public int hashCode() {
        return ((genericComponentType == null) ? 0 : genericComponentType.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericArrayType) {
            GenericArrayType that = (GenericArrayType) obj;
            if (genericComponentType == null) {
                return that.getGenericComponentType() == null;
            } else {
                return genericComponentType.equals(that.getGenericComponentType());
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(genericComponentType.toString());
        sb.append("[]");
        return sb.toString();
    }
}
