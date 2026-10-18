package io.quarkus.arc.impl;

import static io.quarkus.arc.impl.TypeCachePollutionUtils.asParameterizedType;
import static io.quarkus.arc.impl.TypeCachePollutionUtils.isParameterizedType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Set;

/**
 * This code was mainly copied from Weld codebase.
 *
 * This class implements Section 10.3.1 of the CDI specification.
 *
 * @author Jozef Hartinger
 *
 */
final class EventTypeAssignabilityRules {

    private static final EventTypeAssignabilityRules INSTANCE = new EventTypeAssignabilityRules();

    public static EventTypeAssignabilityRules instance() {
        return INSTANCE;
    }

    private EventTypeAssignabilityRules() {
    }

    public boolean matches(Type observedType, Set<? extends Type> eventTypes) {
        for (Type eventType : eventTypes) {
            if (matches(observedType, eventType)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(Type observedType, Type eventType) {
        return matchesNoBoxing(Types.boxedType(observedType), Types.boxedType(eventType));
    }

    boolean matchesNoBoxing(Type observedType, Type eventType) {
        if (Types.isArray(observedType) && Types.isArray(eventType)) {
            final Type observedComponentType = Types.getArrayComponentType(observedType);
            for (Type type : new HierarchyDiscovery(Types.getArrayComponentType(eventType)).getTypeClosure()) {
                if (matchesNoBoxing(observedComponentType, type)) {
                    return true;
                }
            }
            return false;
        }

        if (observedType instanceof TypeVariable<?>) {
            /*
             * An event type is considered assignable to a type variable if the event type is assignable to the upper bound, if
             * any.
             */
            return matches((TypeVariable<?>) observedType, eventType);
        }
        if (observedType instanceof Class<?> && isParameterizedType(eventType)) {
            /*
             * A parameterized event type is considered assignable to a raw observed event type if the raw types are identical.
             */
            return observedType.equals(Types.getRawType(eventType));
        }
        if (isParameterizedType(observedType) && isParameterizedType(eventType)) {
            /*
             * A parameterized event type is considered assignable to a parameterized observed event type if they have identical
             * raw type and for each
             * parameter:
             */
            return matches(asParameterizedType(observedType), asParameterizedType(eventType));
        }
        /*
         * Not explicitly said in the spec but obvious.
         */
        if (observedType instanceof Class<?> && eventType instanceof Class<?>) {
            return observedType.equals(eventType);
        }
        return false;
    }

    private boolean matches(TypeVariable<?> observedType, Type eventType) {
        for (Type bound : BeanTypeAssignabilityRules.instance().getUppermostTypeVariableBounds(observedType)) {
            if (!CovariantTypes.isAssignableFrom(bound, eventType)) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(ParameterizedType observedType, ParameterizedType eventType) {
        if (!observedType.getRawType().equals(eventType.getRawType())) {
            return false;
        }
        if (observedType.getActualTypeArguments().length != eventType.getActualTypeArguments().length) {
            throw new IllegalArgumentException("Invalid argument combination " + observedType + "; " + eventType);
        }
        for (int i = 0; i < observedType.getActualTypeArguments().length; i++) {
            if (!parametersMatch(observedType.getActualTypeArguments()[i], eventType.getActualTypeArguments()[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * A parameterized event type is considered assignable to a parameterized observed event type if they have identical raw
     * type and for each parameter:
     */
    private boolean parametersMatch(Type observedParameter, Type eventParameter) {
        if (Types.isActualType(observedParameter) && Types.isActualType(eventParameter)) {
            /*
             * the observed event type parameter is an actual type with identical raw type to the event type parameter, and, if
             * the type is parameterized, the
             * event type parameter is assignable to the observed event type parameter according to these rules, or
             */
            return matches(observedParameter, eventParameter);
        }
        if (observedParameter instanceof WildcardType && eventParameter instanceof WildcardType) {
            /*
             * both the observed event type parameter and the event type parameter are wildcards, and the event type parameter
             * is assignable to the observed
             * event type
             */
            return CovariantTypes.isAssignableFrom(observedParameter, eventParameter);
        }
        if (observedParameter instanceof WildcardType) {
            /*
             * the observed event type parameter is a wildcard and the event type parameter is assignable to the upper bound, if
             * any, of the wildcard and
             * assignable from the lower bound, if any, of the wildcard, or
             */
            return parametersMatch((WildcardType) observedParameter, eventParameter);
        }
        if (observedParameter instanceof TypeVariable<?>) {
            /*
             * the observed event type parameter is a type variable and the event type parameter is assignable to the upper
             * bound, if any, of the type variable.
             */
            return parametersMatch((TypeVariable<?>) observedParameter, eventParameter);
        }
        return false;
    }

    private boolean parametersMatch(TypeVariable<?> observedParameter, Type eventParameter) {
        for (Type bound : BeanTypeAssignabilityRules.instance().getUppermostTypeVariableBounds(observedParameter)) {
            if (!CovariantTypes.isAssignableFrom(bound, eventParameter)) {
                return false;
            }
        }
        return true;
    }

    private boolean parametersMatch(WildcardType observedParameter, Type eventParameter) {
        return (BeanTypeAssignabilityRules.instance().lowerBoundsOfWildcardMatch(eventParameter, observedParameter)
                && BeanTypeAssignabilityRules.instance().upperBoundsOfWildcardMatch(observedParameter, eventParameter));

    }
}
