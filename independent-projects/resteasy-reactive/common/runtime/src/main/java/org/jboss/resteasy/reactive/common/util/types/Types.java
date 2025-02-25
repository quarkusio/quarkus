package org.jboss.resteasy.reactive.common.util.types;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Type conversions and generic type manipulations
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class Types {

    /**
     * Given a class and an interfaces, go through the class hierarchy to find the interface and return its type arguments.
     *
     * @param classToSearch class
     * @param interfaceToFind interface to find
     * @return type arguments of the interface
     */
    public static Type[] getActualTypeArgumentsOfAnInterface(Class<?> classToSearch, Class<?> interfaceToFind) {
        Type[] types = findParameterizedTypes(classToSearch, interfaceToFind);
        if (types == null)
            throw new RuntimeException("Unable to find type arguments of " + interfaceToFind);
        return types;
    }

    private static final Type[] EMPTY_TYPE_ARRAY = {};

    /**
     * Search for the given interface or class within the root's class/interface hierarchy.
     * If the searched for class/interface is a generic return an array of real types that fill it out.
     *
     * @param root root class
     * @param searchedFor searched class
     * @return for generic class/interface returns array of real types
     */
    public static Type[] findParameterizedTypes(Class<?> root, Class<?> searchedFor) {
        if (searchedFor.isInterface()) {
            return findInterfaceParameterizedTypes(root, null, searchedFor);
        }
        return findClassParameterizedTypes(root, null, searchedFor);
    }

    public static Type[] findClassParameterizedTypes(Class<?> root, ParameterizedType rootType,
            Class<?> searchedForClass) {
        if (Object.class.equals(root))
            return null;

        Map<TypeVariable<?>, Type> typeVarMap = populateParameterizedMap(root, rootType);

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();

        if (superclass.equals(searchedForClass)) {
            return extractTypes(typeVarMap, genericSuper);
        }

        if (genericSuper instanceof ParameterizedType) {
            ParameterizedType intfParam = (ParameterizedType) genericSuper;
            Type[] types = findClassParameterizedTypes(superclass, intfParam, searchedForClass);
            if (types != null) {
                return extractTypeVariables(typeVarMap, types);
            }
        } else {
            Type[] types = findClassParameterizedTypes(superclass, null, searchedForClass);
            if (types != null) {
                return types;
            }
        }
        return null;
    }

    private static Map<TypeVariable<?>, Type> populateParameterizedMap(Class<?> root, ParameterizedType rootType) {
        Map<TypeVariable<?>, Type> typeVarMap = new HashMap<>();
        if (rootType != null) {
            TypeVariable<? extends Class<?>>[] vars = root.getTypeParameters();
            for (int i = 0; i < vars.length; i++) {
                typeVarMap.put(vars[i], rootType.getActualTypeArguments()[i]);
            }
        }
        return typeVarMap;
    }

    public static Type[] findInterfaceParameterizedTypes(Class<?> root, ParameterizedType rootType,
            Class<?> searchedForInterface) {
        Map<TypeVariable<?>, Type> typeVarMap = populateParameterizedMap(root, rootType);

        for (int i = 0; i < root.getInterfaces().length; i++) {
            Class<?> sub = root.getInterfaces()[i];
            Type genericSub = root.getGenericInterfaces()[i];
            if (sub.equals(searchedForInterface)) {
                return extractTypes(typeVarMap, genericSub);
            }
        }

        for (int i = 0; i < root.getInterfaces().length; i++) {
            Type genericSub = root.getGenericInterfaces()[i];
            Class<?> sub = root.getInterfaces()[i];

            Type[] types = recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSub, sub);
            if (types != null)
                return types;
        }
        if (root.isInterface())
            return null;

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();

        return recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSuper, superclass);
    }

    private static Type[] recurseSuperclassForInterface(Class<?> searchedForInterface,
            Map<TypeVariable<?>, Type> typeVarMap, Type genericSub, Class<?> sub) {
        if (genericSub instanceof ParameterizedType) {
            ParameterizedType intfParam = (ParameterizedType) genericSub;
            Type[] types = findInterfaceParameterizedTypes(sub, intfParam, searchedForInterface);
            if (types != null) {
                return extractTypeVariables(typeVarMap, types);
            }
        } else {
            Type[] types = findInterfaceParameterizedTypes(sub, null, searchedForInterface);
            if (types != null) {
                return types;
            }
        }
        return null;
    }

    /**
     * Resolve generic types to actual types.
     *
     * @param typeVarMap The mapping for generic types to actual types.
     * @param types The types to resolve.
     * @return An array of resolved method parameter types in declaration order.
     */
    private static Type[] extractTypeVariables(final Map<TypeVariable<?>, Type> typeVarMap, final Type[] types) {
        final Type[] resolvedMethodParameterTypes = new Type[types.length];

        for (int i = 0; i < types.length; i++) {
            final Type methodParameterType = types[i];

            if (methodParameterType instanceof TypeVariable<?>) {
                resolvedMethodParameterTypes[i] = typeVarMap.get(methodParameterType);
            } else {
                resolvedMethodParameterTypes[i] = methodParameterType;
            }
        }

        return resolvedMethodParameterTypes;
    }

    private static Type[] extractTypes(Map<TypeVariable<?>, Type> typeVarMap, Type genericSub) {
        if (genericSub instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) genericSub;
            Type[] types = param.getActualTypeArguments();

            Type[] returnTypes = extractTypeVariables(typeVarMap, types);
            return returnTypes;
        } else {
            return EMPTY_TYPE_ARRAY;
        }
    }

    public static Type getEffectiveReturnType(Type returnType) {
        if (returnType instanceof Class)
            return returnType;
        if (returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) returnType;
            Type firstTypeArgument = type.getActualTypeArguments()[0];
            Type rawType = type.getRawType();
            if (rawType == CompletionStage.class) {
                return getEffectiveReturnType(firstTypeArgument);
            }
            // do another check, using isAssignableFrom() instead of "==" to catch derived types such as "CompletableFuture" as well
            if (rawType instanceof Class<?> rawClass && CompletionStage.class.isAssignableFrom(rawClass)) {
                return getEffectiveReturnType(firstTypeArgument);
            }
            if (rawType == Uni.class) {
                return getEffectiveReturnType(firstTypeArgument);
            }
            if (rawType == Multi.class) {
                return getEffectiveReturnType(firstTypeArgument);
            }
            if (rawType == RestResponse.class) {
                return getEffectiveReturnType(firstTypeArgument);
            }
            if ("kotlinx.coroutines.flow.Flow".equals(rawType.getTypeName())) { // TODO: this is very ugly and we should probably use some an SPI in order to decouple
                return getEffectiveReturnType(firstTypeArgument);
            }
            return returnType;
        }
        if (returnType instanceof WildcardType) {
            Type[] bounds = ((WildcardType) returnType).getLowerBounds();
            if (bounds.length > 0)
                return getRawType(bounds[0]);
            return getRawType(((WildcardType) returnType).getUpperBounds()[0]);
        }
        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + returnType);
    }

    public static Type getMultipartElementType(Type paramType) {
        if (paramType instanceof Class) {
            if (((Class) paramType).isArray()) {
                return ((Class) paramType).getComponentType();
            }
            return paramType;
        }
        if (paramType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) paramType;
            Type firstTypeArgument = type.getActualTypeArguments()[0];
            if (type.getRawType() == List.class) {
                return firstTypeArgument;
            }
            return paramType;
        }
        if (paramType instanceof GenericArrayType) {
            GenericArrayType type = (GenericArrayType) paramType;
            return type.getGenericComponentType();
        }
        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + paramType);
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class)
            return (Class<?>) type;
        if (type instanceof ParameterizedType)
            return getRawType(((ParameterizedType) type).getRawType());
        if (type instanceof TypeVariable)
            return getRawType(((TypeVariable) type).getBounds()[0]);
        if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getLowerBounds();
            if (bounds.length > 0)
                return getRawType(bounds[0]);
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }
        if (type instanceof GenericArrayType)
            return getRawType(((GenericArrayType) type).getGenericComponentType());
        throw new IllegalArgumentException("Unknown type: " + type);
    }

}
