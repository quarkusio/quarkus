package io.quarkus.cache.redis.runtime;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.types.GenericArrayTypeImpl;
import io.quarkus.runtime.types.ParameterizedTypeImpl;
import io.quarkus.runtime.types.WildcardTypeImpl;

/**
 * A build-time description of a cache key or value type that can be turned back into a {@link Type} at runtime.
 *
 * @param kind the kind of type
 * @param name the class name, for a class or the raw type of a parameterized type
 * @param arguments the type arguments, the array component type, or the wildcard bound
 */
public record TypeRef(Kind kind, String name, List<TypeRef> arguments) {

    public enum Kind {
        CLASS,
        PARAMETERIZED,
        ARRAY,
        WILDCARD,
        WILDCARD_EXTENDS,
        WILDCARD_SUPER
    }

    private static final Map<String, Class<?>> PRIMITIVES = Map.of(
            "boolean", boolean.class,
            "byte", byte.class,
            "short", short.class,
            "int", int.class,
            "long", long.class,
            "float", float.class,
            "double", double.class,
            "char", char.class,
            "void", void.class);

    public TypeRef {
        arguments = List.copyOf(arguments);
    }

    public static TypeRef of(Type type) {
        if (type instanceof Class<?> clazz) {
            return new TypeRef(Kind.CLASS, clazz.getName(), List.of());
        }
        if (type instanceof ParameterizedType parameterized) {
            List<TypeRef> arguments = List.of(parameterized.getActualTypeArguments()).stream().map(TypeRef::of).toList();
            return new TypeRef(Kind.PARAMETERIZED, ((Class<?>) parameterized.getRawType()).getName(), arguments);
        }
        if (type instanceof GenericArrayType array) {
            return new TypeRef(Kind.ARRAY, "", List.of(of(array.getGenericComponentType())));
        }
        if (type instanceof WildcardType wildcard) {
            if (wildcard.getLowerBounds().length > 0) {
                return new TypeRef(Kind.WILDCARD_SUPER, "", List.of(of(wildcard.getLowerBounds()[0])));
            }
            if (wildcard.getUpperBounds().length > 0 && wildcard.getUpperBounds()[0] != Object.class) {
                return new TypeRef(Kind.WILDCARD_EXTENDS, "", List.of(of(wildcard.getUpperBounds()[0])));
            }
            return new TypeRef(Kind.WILDCARD, "", List.of());
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    public Type toType() {
        return switch (kind) {
            case CLASS -> loadClass(name);
            case PARAMETERIZED -> new ParameterizedTypeImpl(loadClass(name),
                    arguments.stream().map(TypeRef::toType).toArray(Type[]::new));
            case ARRAY -> new GenericArrayTypeImpl(arguments.get(0).toType());
            case WILDCARD -> WildcardTypeImpl.defaultInstance();
            case WILDCARD_EXTENDS -> WildcardTypeImpl.withUpperBound(arguments.get(0).toType());
            case WILDCARD_SUPER -> WildcardTypeImpl.withLowerBound(arguments.get(0).toType());
        };
    }

    private static Class<?> loadClass(String name) {
        Class<?> primitive = PRIMITIVES.get(name);
        if (primitive != null) {
            return primitive;
        }
        try {
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load cache type " + name, e);
        }
    }
}
