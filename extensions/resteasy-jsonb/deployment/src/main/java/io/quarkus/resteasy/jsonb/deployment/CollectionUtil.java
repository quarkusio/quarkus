package io.quarkus.resteasy.jsonb.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

public final class CollectionUtil {

    private static final Set<DotName> SUPPORTED_TYPES = new HashSet<>(
            Arrays.asList(DotNames.COLLECTION, DotNames.LIST, DotNames.SET));

    private CollectionUtil() {
    }

    // TODO come up with a better way of determining if the type is supported
    public static boolean isCollection(DotName dotName) {
        return SUPPORTED_TYPES.contains(dotName);
    }

    /**
     * @return the generic type of a collection
     */
    public static Type getGenericType(Type type) {
        if (!isCollection(type.name())) {
            return null;
        }

        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = type.asParameterizedType();

        if (parameterizedType.arguments().size() != 1) {
            return null;
        }

        return parameterizedType.arguments().get(0);
    }
}
