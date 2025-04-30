package io.quarkus.runtime.types;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class ParameterizedTypeImpl implements ParameterizedType, Serializable {

    private static final long serialVersionUID = -3005183010706452884L;

    private final Type[] actualTypeArguments;
    private final Type rawType;
    private final Type ownerType;

    public ParameterizedTypeImpl(Type rawType, Type... actualTypeArguments) {
        this(rawType, actualTypeArguments, null);
    }

    public ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments, Type ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
        this.ownerType = ownerType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return Arrays.copyOf(actualTypeArguments, actualTypeArguments.length);
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(actualTypeArguments) ^ (ownerType == null ? 0 : ownerType.hashCode())
                ^ (rawType == null ? 0 : rawType.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ParameterizedType that) {
            Type thatOwnerType = that.getOwnerType();
            Type thatRawType = that.getRawType();
            return (ownerType == null ? thatOwnerType == null : ownerType.equals(thatOwnerType))
                    && (rawType == null ? thatRawType == null : rawType.equals(thatRawType))
                    && Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
        } else {
            return false;
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (rawType instanceof Class) {
            sb.append(((Class<?>) rawType).getName());
        } else {
            sb.append(rawType);
        }
        if (actualTypeArguments.length > 0) {
            sb.append("<");
            for (Type actualType : actualTypeArguments) {
                if (actualType instanceof Class) {
                    sb.append(((Class<?>) actualType).getName());
                } else {
                    sb.append(actualType);
                }
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(">");
        }
        return sb.toString();
    }
}
