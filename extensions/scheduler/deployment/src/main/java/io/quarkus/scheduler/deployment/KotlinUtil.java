package io.quarkus.scheduler.deployment;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

final class KotlinUtil {

    private static final Type VOID_CLASS = Type.create(SchedulerDotNames.VOID, Type.Kind.CLASS);

    private KotlinUtil() {
    }

    static boolean isSuspendMethod(MethodInfo methodInfo) {
        if (!methodInfo.parameterTypes().isEmpty()) {
            return methodInfo.parameterType(methodInfo.parametersCount() - 1).name()
                    .equals(SchedulerDotNames.CONTINUATION);
        }
        return false;
    }

    static Type determineReturnTypeOfSuspendMethod(MethodInfo methodInfo) {
        Type lastParamType = methodInfo.parameterType(methodInfo.parametersCount() - 1);
        if (lastParamType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            throw new IllegalStateException("Something went wrong during parameter type resolution - expected "
                    + lastParamType + " to be a Continuation with a generic type");
        }
        lastParamType = lastParamType.asParameterizedType().arguments().get(0);
        if (lastParamType.kind() != Type.Kind.WILDCARD_TYPE) {
            throw new IllegalStateException("Something went wrong during parameter type resolution - expected "
                    + lastParamType + " to be a Continuation with a generic type");
        }
        lastParamType = lastParamType.asWildcardType().superBound();
        if (lastParamType.name().equals(SchedulerDotNames.KOTLIN_UNIT)) {
            return VOID_CLASS;
        }
        return lastParamType;
    }
}
