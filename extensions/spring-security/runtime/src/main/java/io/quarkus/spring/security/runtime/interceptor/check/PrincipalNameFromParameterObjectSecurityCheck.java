package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.arc.Arc;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.accessor.StringPropertyAccessor;

/**
 * Instances of these classes are created in order to check if the value of property of method parameter
 * inside a Spring Security expression matches the principal name
 *
 * Access to the property of the object is performed by delegating to a purpose generated
 * accessor
 */
public class PrincipalNameFromParameterObjectSecurityCheck implements SecurityCheck {

    private final int index;
    private final Class<?> expectedParameterClass;
    private final Class<? extends StringPropertyAccessor> stringPropertyAccessorClass;
    private final String propertyName;
    private final CheckType checkType;

    private PrincipalNameFromParameterObjectSecurityCheck(int index, String expectedParameterClass,
            String stringPropertyAccessorClass, String propertyName, CheckType checkType) throws ClassNotFoundException {
        this.index = index;
        this.expectedParameterClass = Class.forName(expectedParameterClass, false,
                Thread.currentThread().getContextClassLoader());
        this.stringPropertyAccessorClass = (Class<? extends StringPropertyAccessor>) Class.forName(stringPropertyAccessorClass,
                false, Thread.currentThread().getContextClassLoader());
        this.propertyName = propertyName;
        this.checkType = checkType;
    }

    public static PrincipalNameFromParameterObjectSecurityCheck of(int index, String expectedParameterClass,
            String stringPropertyAccessorClass, String propertyName, CheckType checkType) {
        try {
            return new PrincipalNameFromParameterObjectSecurityCheck(index, expectedParameterClass, stringPropertyAccessorClass,
                    propertyName, checkType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        if (!expectedParameterClass.equals(parameterValue.getClass())) {
            throw genericNotApplicableException(className, methodName);
        }

        String parameterValueStr = getStringValue(parameterValue);

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

    private String getStringValue(Object parameterValue) {
        return Arc.container().instance(stringPropertyAccessorClass).get().access(parameterValue, propertyName);
    }

    private IllegalStateException genericNotApplicableException(String className, String methodName) {
        return new IllegalStateException(
                "PrincipalNameFromParameterObjectSecurityCheck with index " + index + " cannot be applied to '" + className
                        + "#" + methodName + "'");
    }

    public enum CheckType {
        EQ,
        NEQ
    }
}
