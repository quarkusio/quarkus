package io.quarkus.security.spi.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class MethodDescription {
    private final String className;
    private final String methodName;
    private final String[] parameterTypes;

    private final int hashCode;

    public MethodDescription(String className, String methodName, String[] parameterTypes) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;

        this.hashCode = createHashCode();
    }

    private int createHashCode() {
        int result = Objects.hash(className, methodName);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
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
        return hashCode;
    }

    public static MethodDescription ofMethod(Method method) {
        return new MethodDescription(method.getDeclaringClass().getName(), method.getName(),
                typesAsStrings(method.getParameterTypes()));
    }

    public static String[] typesAsStrings(Class<?>[] parameterTypes) {
        String[] result = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            result[i] = parameterTypes[i].getName();
        }
        return result;
    }
}
