package io.quarkus.arc.processor;

import static org.jboss.jandex.Type.Kind.TYPE_VARIABLE;
import static org.jboss.jandex.Type.Kind.WILDCARD_TYPE;

import java.util.Collections;
import java.util.List;

import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;

/**
 * Logic is mostly equal to that of bean type resolution.
 * However, there are some nuances in parameter matching when it comes to generics.
 */
class DelegateInjectionPointResolverImpl extends BeanResolverImpl {

    DelegateInjectionPointResolverImpl(BeanDeployment deployment) {
        super(deployment);
    }

    @Override
    public boolean matchTypeArguments(Type delegateType, Type beanParameter) {
        // this is the same as for bean types
        if (isActualType(delegateType) && isActualType(beanParameter)) {
            /*
             * the delegate type parameter and the bean type parameter are actual types with identical raw
             * type, and, if the type is parameterized, the bean type parameter is assignable to the delegate
             * type parameter according to these rules, or
             */
            return matches(delegateType, beanParameter);
        }
        // this is the same as for bean types
        if (WILDCARD_TYPE.equals(delegateType.kind()) && isActualType(beanParameter)) {
            /*
             * the delegate type parameter is a wildcard, the bean type parameter is an actual type and the
             * actual type is assignable to the upper bound, if any, of the wildcard and assignable from the
             * lower bound, if any, of the wildcard, or
             */
            return parametersMatch(delegateType.asWildcardType(), beanParameter);
        }
        // this is different from bean type rules
        if (WILDCARD_TYPE.equals(delegateType.kind()) && TYPE_VARIABLE.equals(beanParameter.kind())) {
            /*
             * the delegate type parameter is a wildcard, the bean type parameter is a type variable and the
             * upper bound of the type variable is assignable to the upper bound, if any, of the wildcard and
             * assignable from the lower bound, if any, of the wildcard, or
             */
            return parametersMatch(delegateType.asWildcardType(), beanParameter.asTypeVariable());
        }
        // this is different from bean type rules
        if (TYPE_VARIABLE.equals(delegateType.kind()) && TYPE_VARIABLE.equals(beanParameter.kind())) {
            /*
             * the required type parameter and the bean type parameter are both type variables and the upper bound of the
             * required type parameter is assignable
             * to the upper bound, if any, of the bean type parameter
             */
            return parametersMatch(delegateType.asTypeVariable(), beanParameter.asTypeVariable());
        }
        // this is different to bean type rules
        if (TYPE_VARIABLE.equals(delegateType.kind()) && isActualType(beanParameter)) {
            /*
             * the delegate type parameter is a type variable, the bean type parameter is an actual type, and
             * the actual type is assignable to the upper bound, if any, of the type variable
             */
            return parametersMatch(delegateType.asTypeVariable(), beanParameter);
        }
        return false;
    }

    @Override
    boolean parametersMatch(WildcardType requiredParameter, TypeVariable beanParameter) {
        List<Type> beanParameterBounds = getUppermostTypeVariableBounds(beanParameter);
        if (!lowerBoundsOfWildcardMatch(beanParameterBounds, requiredParameter)) {
            return false;
        }

        List<Type> requiredUpperBounds = Collections.singletonList(requiredParameter.extendsBound());
        // upper bound of the type variable is assignable to the upper bound of the wildcard
        return (boundsMatch(requiredUpperBounds, beanParameterBounds));
    }

    @Override
    boolean parametersMatch(TypeVariable requiredParameter, TypeVariable beanParameter) {
        return boundsMatch(getUppermostTypeVariableBounds(requiredParameter), getUppermostTypeVariableBounds(beanParameter));
    }

    protected boolean parametersMatch(TypeVariable delegateParameter, Type beanParameter) {
        for (Type type : getUppermostTypeVariableBounds(delegateParameter)) {
            if (!beanDeployment.getAssignabilityCheck().isAssignableFrom(type, beanParameter)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected BeanResolver getBeanResolver(BeanInfo bean) {
        return bean.getDeployment().delegateInjectionPointResolver;
    }
}
