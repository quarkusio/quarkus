package io.quarkus.runtime;

import java.lang.reflect.Method;

public class NativeImageFeatureUtils {

    public static Method lookupMethod(Class<?> declaringClass, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method result = declaringClass.getDeclaredMethod(methodName, parameterTypes);
        result.setAccessible(true);
        return result;
    }

    public static Module findModule(String moduleName) {
        if (moduleName == null) {
            return ClassLoader.getSystemClassLoader().getUnnamedModule();
        }
        return ModuleLayer.boot().findModule(moduleName).orElseThrow();
    }
}
