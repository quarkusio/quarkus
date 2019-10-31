package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import io.quarkus.security.Authenticated;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.check.SecurityCheck;

public class SecurityCheckStorageBuilder {
    private final Map<MethodDescription, SecurityCheck> securityChecks = new HashMap<>();

    public void registerAnnotation(String aClass,
            String methodName,
            String[] parameterTypes,
            String securityAnnotation,
            String[] value) {
        securityChecks.put(new MethodDescription(aClass, methodName, parameterTypes),
                determineCheck(securityAnnotation, value));
    }

    public SecurityCheckStorage create() {
        return new SecurityCheckStorage() {
            @Override
            public SecurityCheck getSecurityCheck(Method method) {
                MethodDescription descriptor = new MethodDescription(method.getDeclaringClass().getName(), method.getName(),
                        typesAsStrings(method.getParameterTypes()));
                return securityChecks.get(descriptor);
            }
        };
    }

    private SecurityCheck determineCheck(String securityAnnotation, String[] value) {
        if (DenyAll.class.getName().equals(securityAnnotation)) {
            return new DenyAllCheck();
        }
        if (RolesAllowed.class.getName().equals(securityAnnotation)) {
            return new RolesAllowedCheck(value);
        }
        if (PermitAll.class.getName().equals(securityAnnotation)) {
            return new PermitAllCheck();
        }
        if (Authenticated.class.getName().equals(securityAnnotation)) {
            return new AuthenticatedCheck();
        }
        throw new IllegalArgumentException("Unsupported security check " + securityAnnotation);
    }

    private String[] typesAsStrings(Class[] parameterTypes) {
        String[] result = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            result[i] = parameterTypes[i].getName();
        }
        return result;
    }

    static class MethodDescription {
        private final String className;
        private final String methodName;
        private final String[] parameterTypes;

        public MethodDescription(String aClass, String methodName, String[] parameterTypes) {
            this.className = aClass;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MethodDescription that = (MethodDescription) o;
            return className.equals(that.className) &&
                    methodName.equals(that.methodName) &&
                    Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(className, methodName);
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }
    }
}
