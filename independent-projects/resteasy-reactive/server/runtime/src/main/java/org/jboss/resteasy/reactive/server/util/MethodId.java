package org.jboss.resteasy.reactive.server.util;

import java.util.Arrays;

@SuppressWarnings("rawtypes")
public final class MethodId {

    private MethodId() {
    }

    public static String get(String methodName, String declaringClassName, String... parameterClassNames) {
        return declaringClassName + '#' + methodName + '(' + Arrays.toString(parameterClassNames) + ')';
    }

    public static String get(String methodName, Class declaringClass, Class... parameterClasses) {
        String[] parameterClassNames = new String[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            parameterClassNames[i] = parameterClasses[i].getName();
        }
        return get(methodName, declaringClass.getName(), parameterClassNames);
    }
}
