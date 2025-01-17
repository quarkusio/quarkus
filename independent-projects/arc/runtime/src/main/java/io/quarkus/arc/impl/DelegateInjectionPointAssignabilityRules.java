package io.quarkus.arc.impl;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Code is mostly taken from Weld codebase.
 * The logic is very much the same as with assignability rules for bean types but takes into consideration some
 * special cases for parameter matching when it comes to generics.
 */
final class DelegateInjectionPointAssignabilityRules extends BeanTypeAssignabilityRules {

    private DelegateInjectionPointAssignabilityRules() {
    }

    private static final DelegateInjectionPointAssignabilityRules INSTANCE = new DelegateInjectionPointAssignabilityRules();

    public static DelegateInjectionPointAssignabilityRules instance() {
        return INSTANCE;
    }

    @Override
    protected boolean parametersMatch(Type delegateParameter, Type beanParameter) {
        // this is the same as for bean types
        if (Types.isActualType(delegateParameter) && Types.isActualType(beanParameter)) {
            /*
             * the delegate type parameter and the bean type parameter are actual types with identical raw
             * type, and, if the type is parameterized, the bean type parameter is assignable to the delegate
             * type parameter according to these rules, or
             */
            return matches(delegateParameter, beanParameter);
        }
        // this is the same as for bean types
        if (delegateParameter instanceof WildcardType && Types.isActualType(beanParameter)) {
            /*
             * the delegate type parameter is a wildcard, the bean type parameter is an actual type and the
             * actual type is assignable to the upper bound, if any, of the wildcard and assignable from the
             * lower bound, if any, of the wildcard, or
             */
            return parametersMatch(delegateParameter, beanParameter);
        }
        // this is different to bean type rules
        if (delegateParameter instanceof WildcardType && beanParameter instanceof TypeVariable<?>) {
            /*
             * the delegate type parameter is a wildcard, the bean type parameter is a type variable and the
             * upper bound of the type variable is assignable to the upper bound, if any, of the wildcard and
             * assignable from the lower bound, if any, of the wildcard, or
             */
            return parametersMatch((WildcardType) delegateParameter, (TypeVariable<?>) beanParameter);
        }
        // this is different to bean type rules
        if (delegateParameter instanceof TypeVariable<?> && beanParameter instanceof TypeVariable<?>) {
            /*
             * the delegate type parameter and the bean type parameter are both type variables and the upper
             * bound of the bean type parameter is assignable to the upper bound, if any, of the delegate type
             * parameter, or
             */
            return parametersMatch((TypeVariable<?>) delegateParameter, (TypeVariable<?>) beanParameter);
        }
        // this is different to bean type rules
        if (delegateParameter instanceof TypeVariable<?> && Types.isActualType(beanParameter)) {
            /*
             * the delegate type parameter is a type variable, the bean type parameter is an actual type, and
             * the actual type is assignable to the upper bound, if any, of the type variable
             */
            return parametersMatch((TypeVariable<?>) delegateParameter, beanParameter);
        }
        /*
         * this is not defined by the specification but is here to retain backward compatibility with previous
         * versions of Weld
         * see CDITCK-430
         */
        if (Object.class.equals(delegateParameter) && beanParameter instanceof TypeVariable<?>) {
            TypeVariable<?> beanParameterVariable = (TypeVariable<?>) beanParameter;
            return Object.class.equals(beanParameterVariable.getBounds()[0]);
        }

        return false;
    }

    @Override
    protected boolean parametersMatch(WildcardType delegateParameter, TypeVariable<?> beanParameter) {
        Type[] beanParameterBounds = getUppermostTypeVariableBounds(beanParameter);
        if (!lowerBoundsOfWildcardMatch(beanParameterBounds, delegateParameter)) {
            return false;
        }

        Type[] requiredUpperBounds = delegateParameter.getUpperBounds();
        // upper bound of the type variable is assignable to the upper bound of the wildcard
        return boundsMatch(requiredUpperBounds, beanParameterBounds);
    }

    @Override
    protected boolean parametersMatch(TypeVariable<?> delegateParameter, TypeVariable<?> beanParameter) {
        return boundsMatch(getUppermostTypeVariableBounds(delegateParameter), getUppermostTypeVariableBounds(beanParameter));
    }

    protected boolean parametersMatch(TypeVariable<?> delegateParameter, Type beanParameter) {
        for (Type type : getUppermostTypeVariableBounds(delegateParameter)) {
            if (!CovariantTypes.isAssignableFrom(type, beanParameter)) {
                return false;
            }
        }
        return true;
    }
}
