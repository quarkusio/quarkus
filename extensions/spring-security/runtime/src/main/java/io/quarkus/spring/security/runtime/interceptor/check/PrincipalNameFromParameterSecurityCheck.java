package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;

/**
 * Instances of this classes are created in order to check if a method parameter
 * inside a Spring Security expression matches the principal name
 *
 * Access to the property of the object is performed by delegating to a purpose generated
 * accessor
 */
public class PrincipalNameFromParameterSecurityCheck implements SecurityCheck {

    private final int index;
    private final CheckType checkType;

    private PrincipalNameFromParameterSecurityCheck(int index, CheckType checkType) {
        this.index = index;
        this.checkType = checkType;
    }

    public static PrincipalNameFromParameterSecurityCheck of(int index, CheckType checkType) {
        return new PrincipalNameFromParameterSecurityCheck(index, checkType);
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        doApply(identity, parameters, method.getDeclaringClass().getName(), method.getName());
    }

    @Override
    public void apply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        doApply(identity, parameters, methodDescription.getClassName(), methodDescription.getMethodName());
    }

    private void doApply(SecurityIdentity identity, Object[] parameters, String className, String methodName) {
        if (index > parameters.length - 1) {
            throw genericNotApplicableException(className, methodName);
        }
        Object parameterValue = parameters[index];
        if (!(parameterValue instanceof String)) {
            throw genericNotApplicableException(className, methodName);
        }
        String parameterValueStr = (String) parameterValue;

        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }

        String name = identity.getPrincipal().getName();
        if (checkType == CheckType.EQ) {
            if (!name.equals(parameterValueStr)) {
                throw new ForbiddenException();
            }
        } else if (checkType == CheckType.NEQ) {
            if (name.equals(parameterValueStr)) {
                throw new ForbiddenException();
            }
        }
    }

    private IllegalStateException genericNotApplicableException(String className, String methodName) {
        return new IllegalStateException(
                "PrincipalNameFromParameterSecurityCheck with index " + index + " cannot be applied to '" + className + "#"
                        + methodName + "'");
    }

    public enum CheckType {
        EQ,
        NEQ
    }
}
