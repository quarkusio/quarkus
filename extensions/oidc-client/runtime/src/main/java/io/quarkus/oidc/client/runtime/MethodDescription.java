package io.quarkus.oidc.client.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public record MethodDescription(String className, String methodName, String[] parameterTypes) {

    public static MethodDescription ofMethod(Method method) {
        return new MethodDescription(method.getDeclaringClass().getName(), method.getName(),
                typesAsStrings(method.getParameterTypes()));
    }

    private static String[] typesAsStrings(Class<?>[] parameterTypes) {
        String[] result = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            result[i] = parameterTypes[i].getName();
        }
        return result;
    }

    // we replace standard equals and hash code as we need the deep equals for array
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        MethodDescription that = (MethodDescription) o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName)
                && Objects.deepEquals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, Arrays.hashCode(parameterTypes));
    }
}
