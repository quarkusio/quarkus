package io.quarkus.arc.processor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;

// first attempt at creating a type system API for Jandex, includes the bare minimum for validating decorators
// it should be replaced by a proper API in Jandex one day, see https://github.com/smallrye/jandex/issues/625
final class JandexTypeSystem {
    private final IndexView index;

    static JandexTypeSystem of(IndexView index) {
        Objects.requireNonNull(index);
        return new JandexTypeSystem(index);
    }

    private JandexTypeSystem(IndexView index) {
        this.index = index;
    }

    /**
     * Returns the given {@code type} and all its supertypes. In case a supertype is parameterized,
     * the type parameters of the supertype's declaration are replaced by the type arguments
     * of the supertype.
     * <p>
     * The result order is:
     * <ol>
     * <li>the given {@code type} (always)</li>
     * <li>all superclass types in the bottom-up order (if the given {@code type} is a class type)</li>
     * <li>superinterface types, in an unspecified order (if {@code skipInterfaces == false})</li>
     * </ol>
     */
    List<Type> typeWithSuperTypes(Type type, boolean skipInterfaces) {
        Objects.requireNonNull(type);
        if (type.kind() != Type.Kind.CLASS && type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            throw new IllegalArgumentException("Type must be class or parameterized, got " + type.kind() + ": " + type);
        }

        List<Type> result = new ArrayList<>();

        result.add(type);
        ClassInfo clazz = requireClass(type);
        Function<String, Type> substitution = createSubstitution(clazz, type);

        if (!clazz.isInterface()) {
            while (!clazz.name().equals(DotName.OBJECT_NAME)) {
                type = substituteTypeVariables(clazz.superClassType(), substitution);
                result.add(type);
                clazz = requireClass(type);
                substitution = createSubstitution(clazz, type);
            }
        }

        // at the moment, `result` contains the original type and all its superclass types
        // (note that `result` contains only the original type if it is an interface type)

        if (skipInterfaces) {
            return result;
        }

        // it is enough to process each interface exactly once, because a type in Java
        // may not inherit from multiple different instantiations of the same generic type
        Set<DotName> seen = new HashSet<>();
        Deque<Type> worklist = new ArrayDeque<>(result);
        while (!worklist.isEmpty()) {
            Type item = worklist.removeFirst();
            clazz = requireClass(item);
            substitution = createSubstitution(clazz, item);

            for (Type interfaceType : clazz.interfaceTypes()) {
                if (seen.add(interfaceType.name())) {
                    interfaceType = substituteTypeVariables(interfaceType, substitution);
                    result.add(interfaceType);
                    worklist.add(interfaceType);
                }
            }
        }

        return result;
    }

    record MethodKey(String name, List<Type> parameterTypes, Type returnType) {
        public void appendTo(StringBuilder str) {
            str.append(returnType).append(" ").append(name).append("(");
            boolean first = true;
            for (Type parameterType : parameterTypes) {
                if (!first) {
                    str.append(", ");
                }
                str.append(parameterType);
                first = false;
            }
            str.append(")");
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            appendTo(str);
            return str.toString();
        }
    }

    /**
     * Returns all methods declared directly in the class referred to by the given {@code type}.
     * If the given {@code type} is a parameterized type, the class's type parameters in method
     * signatures are replaced by the type arguments in the given {@code type}.
     * <p>
     * Methods that do not pass the given {@code filter} are omitted.
     */
    List<MethodKey> methods(Type type, Predicate<MethodInfo> filter) {
        ClassInfo clazz = requireClass(type);
        Function<String, Type> substitution = createSubstitution(clazz, type);

        List<MethodInfo> methods = clazz.methods();
        List<MethodKey> result = new ArrayList<>(methods.size());
        for (MethodInfo method : methods) {
            if (!filter.test(method)) {
                continue;
            }
            // we ignore method type parameters here, though there's not much we can do
            Type returnType = substituteTypeVariables(method.returnType(), substitution);
            Type[] parameterTypes = new Type[method.parametersCount()];
            for (int i = 0; i < method.parametersCount(); i++) {
                parameterTypes[i] = substituteTypeVariables(method.parameterType(i), substitution);
            }
            result.add(new MethodKey(method.name(), Arrays.asList(parameterTypes), returnType));
        }
        return result;
    }

    private static final Type ERASURE = ClassType.create("ERASURE PLACEHOLDER");

    private Function<String, Type> createSubstitution(ClassInfo declaration, Type type) {
        if (!declaration.name().equals(type.name())) {
            throw new IllegalArgumentException("Type must refer to the given declaration, got "
                    + declaration.name() + " and " + type);
        }

        if (declaration.typeParameters().isEmpty()) {
            return ignored -> null;
        }
        if (type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return ignored -> ERASURE;
        }

        List<TypeVariable> typeParameters = declaration.typeParameters();
        List<Type> typeArguments = type.asParameterizedType().arguments();
        if (typeParameters.size() != typeArguments.size()) {
            // this branch is never taken when types come from actually compiled class files
            throw new IllegalArgumentException("Declaration has " + typeParameters.size()
                    + " type parameters, but its type use has " + typeArguments.size() + " type arguments");
        }

        if (typeParameters.isEmpty()) {
            return ignored -> null;
        } else if (typeParameters.size() == 1) {
            String typeParamId = typeParameters.get(0).identifier();
            Type typeArg = typeArguments.get(0);
            return id -> id.equals(typeParamId) ? typeArg : null;
        } else if (typeParameters.size() == 2) {
            String typeParamId1 = typeParameters.get(0).identifier();
            Type typeArg1 = typeArguments.get(0);
            String typeParamId2 = typeParameters.get(1).identifier();
            Type typeArg2 = typeArguments.get(1);
            return id -> {
                if (id.equals(typeParamId1)) {
                    return typeArg1;
                } else if (id.equals(typeParamId2)) {
                    return typeArg2;
                } else {
                    return null;
                }
            };
        } else {
            Map<String, Type> data = new HashMap<>((int) (typeParameters.size() * 1.5));
            for (int i = 0; i < typeParameters.size(); i++) {
                data.put(typeParameters.get(i).identifier(), typeArguments.get(i));
            }
            return data::get;
        }
    }

    /**
     * Returns this type after performing a type variable substitution.
     * <p>
     * Type variables are substituted for other types provided by the given {@code substitution} function.
     * The function is supposed to return another type for a given type variable identifier.
     * If the substitution function returns {@code null} for some type variable identifier,
     * no substitution happens and the type variable is used unmodified. If the substitution function
     * returns {@link #ERASURE} for some type variable identifier, the type variable is substituted
     * for its erasure (left-most bound, or {@code java.lang.Object} if unbounded).
     *
     * @param substitution the substitution function; must not be {@code null}
     * @return this type after type variable substitution
     */
    private Type substituteTypeVariables(Type type, Function<String, Type> substitution) {
        Objects.requireNonNull(substitution);
        Type result = null;
        switch (type.kind()) {
            case VOID:
            case PRIMITIVE:
            case CLASS:
                // no need to substitute anything
                return type;
            case ARRAY: {
                ArrayType arrayType = type.asArrayType();
                Type substitutedConstituent = substituteTypeVariables(arrayType.constituent(), substitution);
                return ArrayType.create(substitutedConstituent, arrayType.dimensions());
            }
            case PARAMETERIZED_TYPE: {
                ParameterizedType parameterizedType = type.asParameterizedType();
                List<Type> typeArgs = parameterizedType.arguments();
                Type[] substitutedTypeArgs = new Type[typeArgs.size()];
                for (int i = 0; i < typeArgs.size(); i++) {
                    substitutedTypeArgs[i] = substituteTypeVariables(typeArgs.get(i), substitution);
                }
                Type substitutedOwner = parameterizedType.owner() != null
                        ? substituteTypeVariables(parameterizedType.owner(), substitution)
                        : null;
                return ParameterizedType.create(type.name(), substitutedTypeArgs, substitutedOwner);
            }
            case TYPE_VARIABLE: {
                return callSubstitution(type.asTypeVariable().identifier(), type, substitution);
            }
            case TYPE_VARIABLE_REFERENCE: {
                return callSubstitution(type.asTypeVariableReference().identifier(), type, substitution);
            }
            case UNRESOLVED_TYPE_VARIABLE: {
                return callSubstitution(type.asUnresolvedTypeVariable().identifier(), type, substitution);
            }
            case WILDCARD_TYPE: {
                WildcardType wildcardType = type.asWildcardType();
                boolean isExtends = wildcardType.superBound() == null;
                boolean hasImplicitObjectBound = isExtends && wildcardType.extendsBound() == ClassType.OBJECT_TYPE;
                if (hasImplicitObjectBound) {
                    // no need to substitute anything
                    return type;
                } else if (isExtends) {
                    return WildcardType.createUpperBound(substituteTypeVariables(wildcardType.extendsBound(), substitution));
                } else {
                    return WildcardType.createLowerBound(substituteTypeVariables(wildcardType.superBound(), substitution));
                }
            }
            default:
                throw new IllegalArgumentException("Unsupported type: " + this);
        }
    }

    private Type callSubstitution(String typeVarId, Type typeVar, Function<String, Type> substitution) {
        Type substituted = substitution.apply(typeVarId);
        if (substituted == null) {
            return typeVar;
        } else if (substituted == ERASURE) {
            // the leftmost bound of a type variable is either a class type or a type variable
            // since type variable erases to the erasure of its leftmost bound, it is eventually always a class type
            return ClassType.create(typeVar.name());
        } else {
            return substituted;
        }
    }

    private ClassInfo requireClass(Type type) {
        return requireClass(type.name());
    }

    private ClassInfo requireClass(DotName name) {
        ClassInfo clazz = index.getClassByName(name);
        if (clazz == null) {
            throw new IllegalStateException("Index does not include class " + name);
        }
        return clazz;
    }
}
