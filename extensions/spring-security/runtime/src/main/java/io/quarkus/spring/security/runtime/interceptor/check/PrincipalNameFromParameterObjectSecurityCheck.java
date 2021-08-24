package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.arc.Arc;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.accessor.StringPropertyAccessor;

/**
 * Instances of this classes are created in order to check if the value of property of method parameter
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

    private PrincipalNameFromParameterObjectSecurityCheck(int index, String expectedParameterClass,
            String stringPropertyAccessorClass, String propertyName) throws ClassNotFoundException {
        this.index = index;
        this.expectedParameterClass = Class.forName(expectedParameterClass, false,
                Thread.currentThread().getContextClassLoader());
        this.stringPropertyAccessorClass = (Class<? extends StringPropertyAccessor>) Class.forName(stringPropertyAccessorClass,
                false, Thread.currentThread().getContextClassLoader());
        this.propertyName = propertyName;
    }

    public static PrincipalNameFromParameterObjectSecurityCheck of(int index, String expectedParameterClass,
            String stringPropertyAccessorClass, String propertyName) {
        try {
            return new PrincipalNameFromParameterObjectSecurityCheck(index, expectedParameterClass, stringPropertyAccessorClass,
                    propertyName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        if (index > parameters.length - 1) {
            throw genericNotApplicableException(method);
        }
        Object parameterValue = parameters[index];
        if (!expectedParameterClass.equals(parameterValue.getClass())) {
            throw genericNotApplicableException(method);
        }

        String parameterValueStr = getStringValue(parameterValue);

        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }

        String name = identity.getPrincipal().getName();
        if (!name.equals(parameterValueStr)) {
            throw new ForbiddenException();
        }
    }

    private String getStringValue(Object parameterValue) {
        return Arc.container().instance(stringPropertyAccessorClass).get().access(parameterValue, propertyName);
    }

    private IllegalStateException genericNotApplicableException(Method method) {
        return new IllegalStateException(
                "PrincipalNameFromParameterObjectSecurityCheck with index " + index + " cannot be applied to " + method);
    }
}
