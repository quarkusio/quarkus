package io.quarkus.core.deployment.service.impl;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import io.smallrye.serial.Serialized;
import io.smallrye.serial.spi.ObjectSerializer;

/**
 * A custom {@link ObjectSerializer} for {@code smallrye-serial} that intercepts
 * {@link ParameterizedType}, {@link WildcardType} and {@link GenericArrayType}
 * objects, producing a {@link SerializedJavaType}. ({@link Class} objects
 * are supported by {@code smallrye-serial} natively, and we don't need to
 * support {@link java.lang.reflect.TypeVariable} yet.)
 * <p>
 * This runs between the lowest class serialization priority and highest value
 * serialization priority.
 */
final class JavaTypeSerializer implements ObjectSerializer {
    /**
     * Singleton instance.
     */
    static final JavaTypeSerializer INSTANCE = new JavaTypeSerializer();

    private JavaTypeSerializer() {
    }

    @Override
    public Serialized serialize(Context ctxt, Object object) throws IOException {
        return switch (object) {
            case ParameterizedType pt -> {
                Type[] typeArgs = pt.getActualTypeArguments();
                Serialized[] serializedTypeArgs = new Serialized[typeArgs.length];
                for (int i = 0; i < typeArgs.length; i++) {
                    serializedTypeArgs[i] = ctxt.serialize(typeArgs[i]);
                }
                yield new SerializedJavaType.OfParameterizedType(ctxt.serialize(pt.getRawType()),
                        serializedTypeArgs, ctxt.serialize(pt.getOwnerType()));
            }
            case WildcardType wt -> {
                if (wt.getLowerBounds().length == 1) {
                    Serialized serializedBound = ctxt.serialize(wt.getLowerBounds()[0]);
                    yield new SerializedJavaType.OfWildcardType(SerializedJavaType.OfWildcardType.LOWER_BOUND, serializedBound);
                } else if (wt.getUpperBounds().length == 1) {
                    Type bound = wt.getUpperBounds()[0];
                    int kind = bound == Object.class
                            ? SerializedJavaType.OfWildcardType.UNBOUNDED
                            : SerializedJavaType.OfWildcardType.UPPER_BOUND;
                    Serialized serializedBound = ctxt.serialize(bound);
                    yield new SerializedJavaType.OfWildcardType(kind, serializedBound);
                } else {
                    throw new IllegalArgumentException("Invalid wildcard type: " + wt);
                }
            }
            case GenericArrayType gat -> {
                Serialized serializedComponent = ctxt.serialize(gat.getGenericComponentType());
                yield new SerializedJavaType.OfGenericArrayType(serializedComponent);
            }
            default -> ctxt.next();
        };
    }

    @Override
    public int priority() {
        return PRIORITY_REPLACE + 500;
    }
}
