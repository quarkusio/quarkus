package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

public class SecurityCheckStorageBuilder {
    private final Map<MethodDescription, SecurityCheck> securityChecks = new HashMap<>();

    public void registerCheck(String className,
            String methodName,
            String[] parameterTypes,
            SecurityCheck securityCheck) {
        securityChecks.put(new MethodDescription(className, methodName, parameterTypes), securityCheck);
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

    private String[] typesAsStrings(Class<?>[] parameterTypes) {
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

        public MethodDescription(String className, String methodName, String[] parameterTypes) {
            this.className = className;
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
