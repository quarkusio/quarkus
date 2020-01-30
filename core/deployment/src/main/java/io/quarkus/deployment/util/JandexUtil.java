package io.quarkus.deployment.util;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

public final class JandexUtil {

    private static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    private JandexUtil() {
    }

    /**
     * Returns the captured generic types of an interface given a class that at some point in the class
     * hierarchy implements the interface.
     *
     * The list contains the types in the same order as they are generic parameters defined on the interface
     *
     * A result is only returned if and only if all the generics where captured. If any of them where not defined by the class
     * an exception is thrown.
     *
     * Also note that all parts of the class/interface hierarchy must be in the supplied index
     *
     * As an example, imagine the following class:
     *
     * <pre>
     *
     * class MyList implements List&lt;String&gt; {
     *     ...
     * }
     *
     * </pre>
     *
     * If we call
     *
     * <pre>
     *
     * JandexUtil.resolveTypeParameters(DotName.createSimple(MyList.class.getName()),
     *         DotName.createSimple(List.class.getName()), index)
     *
     * </pre>
     *
     * then the result will contain a single element of class ClassType whose name() would return a DotName for String
     */
    public static List<Type> resolveTypeParameters(DotName input, DotName target, IndexView index) {
        final ClassInfo inputClassInfo = fetchFromIndex(input, index);

        final ClassInfo targetClassInfo = fetchFromIndex(target, index);

        final RecursiveMatchResult recursiveMatchResult = matchParametersRecursively(inputClassInfo, target,
                Modifier.isInterface(targetClassInfo.flags()), index, new LinkedList<>());
        if (recursiveMatchResult == null) {
            return Collections.emptyList();
        }
        final List<Type> result = resolveTypeParameters(recursiveMatchResult);

        if (result.size() != recursiveMatchResult.argumentsOfMatch.size()) {
            throw new IllegalStateException("Unable to properly match generic types");
        }

        return result;
    }

    private static List<Type> resolveTypeParameters(RecursiveMatchResult recursiveMatchResult) {
        final List<Type> result = new ArrayList<>();
        for (int i = 0; i < recursiveMatchResult.argumentsOfMatch.size(); i++) {
            final Type argument = recursiveMatchResult.argumentsOfMatch.get(i);
            if (argument instanceof ClassType) {
                result.add(argument);
            } else if (argument instanceof ParameterizedType) {
                ParameterizedType argumentParameterizedType = argument.asParameterizedType();
                List<Type> resolvedTypes = new ArrayList<>(argumentParameterizedType.arguments().size());
                for (Type argType : argumentParameterizedType.arguments()) {
                    if (argType instanceof TypeVariable) {
                        resolvedTypes.add(findTypeFromTypeVariable(recursiveMatchResult, argType.asTypeVariable()));
                    } else {
                        resolvedTypes.add(argType);
                    }
                }
                result.add(ParameterizedType.create(argumentParameterizedType.name(), resolvedTypes.toArray(new Type[0]),
                        argumentParameterizedType.owner()));
            } else if (argument instanceof TypeVariable) {
                Type typeFromTypeVariable = findTypeFromTypeVariable(recursiveMatchResult, argument.asTypeVariable());
                if (typeFromTypeVariable != null) {
                    result.add(typeFromTypeVariable);
                }
            } else if (argument instanceof ArrayType) {
                ArrayType argumentAsArrayType = argument.asArrayType();
                Type componentType = argumentAsArrayType.component();
                if (componentType instanceof TypeVariable) { // should always be the case
                    Type typeFromTypeVariable = findTypeFromTypeVariable(recursiveMatchResult, componentType.asTypeVariable());
                    if (typeFromTypeVariable != null) {
                        result.add(ArrayType.create(typeFromTypeVariable, argumentAsArrayType.dimensions()));
                    }
                }
            }
        }
        return result;
    }

    private static Type findTypeFromTypeVariable(RecursiveMatchResult recursiveMatchResult, TypeVariable typeVariable) {
        String unmatchedParameter = typeVariable.identifier();

        for (RecursiveMatchLevel recursiveMatchLevel : recursiveMatchResult.recursiveMatchLevels) {
            Type matchingCapturedType = null;
            for (int j = 0; j < recursiveMatchLevel.definitions.size(); j++) {
                final Type definition = recursiveMatchLevel.definitions.get(j);
                if ((definition instanceof TypeVariable)
                        && unmatchedParameter.equals(definition.asTypeVariable().identifier())) {
                    matchingCapturedType = recursiveMatchLevel.captures.get(j);
                    break; // out of the definitions loop
                }
            }
            // at this point their MUST be a match, if there isn't we have made some mistake in the implementation
            if (matchingCapturedType == null) {
                throw new IllegalStateException("Error retrieving generic types");
            }
            if (isDirectlyHandledType(matchingCapturedType)) {
                // search is over
                return matchingCapturedType;
            }
            if (matchingCapturedType instanceof TypeVariable) {
                // continue the search in the lower levels using the new name
                unmatchedParameter = matchingCapturedType.asTypeVariable().identifier();
            }
        }

        return null;
    }

    private static boolean isDirectlyHandledType(Type matchingCapturedType) {
        return (matchingCapturedType instanceof ClassType) ||
                (matchingCapturedType instanceof ParameterizedType);
    }

    private static RecursiveMatchResult matchParametersRecursively(ClassInfo inputClassInfo, DotName target,
            boolean isTargetAnInterface,
            IndexView index, List<RecursiveMatchLevel> visitedTypes) {

        if (isTargetAnInterface) {
            final List<Type> interfaceTypes = inputClassInfo.interfaceTypes();
            final List<Type> nonMatchingInterfaces = new ArrayList<>();
            for (Type interfaceType : interfaceTypes) {
                if (target.equals(interfaceType.name())) {
                    return getRecursiveMatchResult(visitedTypes, interfaceType);
                }
                // we only check the other interfaces if we didn't find anything
                // this ensures that we also use the "closest" path to the target interface
                nonMatchingInterfaces.add(interfaceType);
            }

            // go through the non matching interfaces and check if any of one of them extends the target
            for (Type otherInterface : nonMatchingInterfaces) {
                final RecursiveMatchResult recursiveMatchResult = matchParametersRecursively(
                        fetchFromIndex(otherInterface.name(), index), target, isTargetAnInterface, index,
                        addArgumentIfNeeded(otherInterface, index, visitedTypes));
                if (recursiveMatchResult != null) {
                    return recursiveMatchResult;
                }
            }
        }

        // check if the super class is a match - if not keep going up the type hierarchy until we reach object
        final Type superClassType = inputClassInfo.superClassType();
        if (target.equals(superClassType.name())) {
            return getRecursiveMatchResult(visitedTypes, superClassType);
        }
        if (OBJECT.equals(superClassType.name())) {
            return null;
        }
        return matchParametersRecursively(fetchFromIndex(superClassType.name(), index), target, isTargetAnInterface, index,
                addArgumentIfNeeded(superClassType, index, visitedTypes));
    }

    private static RecursiveMatchResult getRecursiveMatchResult(List<RecursiveMatchLevel> visitedTypes, Type superClassType) {
        if (superClassType instanceof ParameterizedType) {
            return new RecursiveMatchResult(superClassType.asParameterizedType().arguments(), visitedTypes);
        }
        return null;
    }

    private static ClassInfo fetchFromIndex(DotName dotName, IndexView index) {
        final ClassInfo classInfo = index.getClassByName(dotName);
        if (classInfo == null) {
            throw new IllegalArgumentException("Class " + dotName + " was not found in the index");
        }
        return classInfo;
    }

    private static List<RecursiveMatchLevel> addArgumentIfNeeded(Type type, IndexView index,
            List<RecursiveMatchLevel> visitedTypes) {
        final List<RecursiveMatchLevel> newVisitedTypes = new LinkedList<>(visitedTypes);
        if (type instanceof ParameterizedType) {
            final ClassInfo classInfo = fetchFromIndex(type.name(), index);
            final RecursiveMatchLevel recursiveMatchLevel = new RecursiveMatchLevel(classInfo.typeParameters(),
                    type.asParameterizedType().arguments());
            newVisitedTypes.add(0, recursiveMatchLevel);
        } else if (type instanceof ClassType) {
            // we need to check if the class contains any bounds
            final ClassInfo classInfo = index.getClassByName(type.name());
            if ((classInfo != null) && !classInfo.typeParameters().isEmpty()) {
                // in this case we just use the first bound as the capture type
                // TODO do we need something more sophisticated than this?
                List<Type> captures = new ArrayList<>(classInfo.typeParameters().size());
                for (TypeVariable typeParameter : classInfo.typeParameters()) {
                    captures.add(typeParameter.bounds().get(0));
                }
                final RecursiveMatchLevel recursiveMatchLevel = new RecursiveMatchLevel(classInfo.typeParameters(), captures);
                newVisitedTypes.add(0, recursiveMatchLevel);
            }
        }
        return newVisitedTypes;
    }

    private static class RecursiveMatchResult {
        private final List<Type> argumentsOfMatch;
        private final List<RecursiveMatchLevel> recursiveMatchLevels;

        public RecursiveMatchResult(List<Type> argumentsOfMatch, List<RecursiveMatchLevel> recursiveMatchLevels) {
            this.argumentsOfMatch = argumentsOfMatch;
            this.recursiveMatchLevels = recursiveMatchLevels;
        }
    }

    private static class RecursiveMatchLevel {
        private final List<? extends Type> definitions;
        private final List<? extends Type> captures;

        public RecursiveMatchLevel(List<? extends Type> definitions, List<? extends Type> captures) {
            this.definitions = definitions;
            this.captures = captures;
        }
    }
}
