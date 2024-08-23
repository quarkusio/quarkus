package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

public class KotlinUtils {
    public static boolean isKotlinClass(ClassInfo clazz) {
        return clazz.hasDeclaredAnnotation(KotlinDotNames.METADATA);
    }

    public static boolean isKotlinMethod(MethodInfo method) {
        return isKotlinClass(method.declaringClass());
    }

    public static boolean isKotlinSuspendMethod(MethodInfo method) {
        if (!isKotlinMethod(method)) {
            return false;
        }
        if (method.parametersCount() == 0) {
            return false;
        }

        Type lastParameter = method.parameterType(method.parametersCount() - 1);
        return KotlinDotNames.CONTINUATION.equals(lastParameter.name());
    }

    public static boolean isKotlinContinuationParameter(MethodParameterInfo parameter) {
        return isKotlinSuspendMethod(parameter.method()) && KotlinDotNames.CONTINUATION.equals(parameter.type().name());
    }

    public static boolean isNoninterceptableKotlinMethod(MethodInfo method) {
        // the Kotlin compiler generates somewhat streamlined bytecode when it determines
        // that a `suspend` method cannot be overridden, and that bytecode doesn't work
        // well with subclassing-based interception
        //
        // a `suspend` method can be overridden when it is `open` and is declared in an `open` class
        return isKotlinSuspendMethod(method)
                && (Modifier.isFinal(method.flags()) || Modifier.isFinal(method.declaringClass().flags()));
    }

    public static Type getKotlinSuspendMethodResult(MethodInfo method) {
        if (!isKotlinSuspendMethod(method)) {
            throw new IllegalArgumentException("Not a suspend function: " + method);
        }

        Type lastParameter = method.parameterType(method.parametersCount() - 1);
        if (lastParameter.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            throw new IllegalArgumentException("Continuation parameter type not parameterized: " + lastParameter);
        }
        Type resultType = lastParameter.asParameterizedType().arguments().get(0);
        if (resultType.kind() != Type.Kind.WILDCARD_TYPE) {
            throw new IllegalArgumentException("Continuation parameter type argument not wildcard: " + resultType);
        }
        Type lowerBound = resultType.asWildcardType().superBound();
        if (lowerBound == null) {
            throw new IllegalArgumentException("Continuation parameter type argument without lower bound: " + resultType);
        }
        return lowerBound;
    }
}
