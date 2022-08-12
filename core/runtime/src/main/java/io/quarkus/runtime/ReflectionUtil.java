package io.quarkus.runtime;

import java.lang.reflect.Method;

public class ReflectionUtil {

    public static Method lookupMethod(Class<?> declaringClass, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method result = declaringClass.getDeclaredMethod(methodName, parameterTypes);
        result.setAccessible(true);
        return result;
    }
}
