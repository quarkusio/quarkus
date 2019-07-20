package io.quarkus.resteasy.jsonb.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

public final class MapUtil {

    private static final Set<DotName> SUPPORTED_TYPES = new HashSet<>(
            Arrays.asList(DotNames.MAP, DotNames.HASHMAP));

    private MapUtil() {
    }

    // TODO come up with a better way of determining if the type is supported
    public static boolean isMap(DotName dotName) {
        return SUPPORTED_TYPES.contains(dotName);
    }

    /**
     * @return the generic type of a collection
     */
    public static MapTypes getGenericType(Type type) {
        if (!isMap(type.name())) {
            return null;
        }

        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = type.asParameterizedType();

        if (parameterizedType.arguments().size() != 2) {
            return null;
        }

        return new MapTypes(parameterizedType.arguments().get(0), parameterizedType.arguments().get(1));
    }

    public static class MapTypes {
        private final Type keyType;
        private final Type valueType;

        public MapTypes(Type keyType, Type valueType) {
            this.keyType = keyType;
            this.valueType = valueType;
        }

        public Type getKeyType() {
            return keyType;
        }

        public Type getValueType() {
            return valueType;
        }
    }
}
