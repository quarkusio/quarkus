package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_ALL;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;

import java.util.List;
import java.util.Optional;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.cache.deployment.exception.IllegalReturnTypeException;
import io.quarkus.cache.deployment.exception.MultipleCacheAnnotationsException;

public class CacheMethodValidator {

    public static void validateAnnotations(AnnotationStore annotationStore, BeanInfo bean, MethodInfo method,
            List<Throwable> throwables) {
        int methodAnnotationsCount = 0;
        if (annotationStore.getAnnotation(method, CACHE_RESULT) != null) {
            checkVoidReturnType(CACHE_RESULT, bean, method).ifPresent(throwables::add);
            methodAnnotationsCount++;
        }
        if (annotationStore.getAnnotation(method, CACHE_INVALIDATE) != null) {
            methodAnnotationsCount++;
        }
        if (annotationStore.getAnnotation(method, CACHE_INVALIDATE_ALL) != null) {
            methodAnnotationsCount++;
        }
        if (methodAnnotationsCount > 1) {
            String error = "Multiple incompatible cache annotations found on the same method";
            throwables.add(new MultipleCacheAnnotationsException(buildExceptionMessage(error, bean.getBeanClass(), method)));
        }
    }

    private static Optional<IllegalReturnTypeException> checkVoidReturnType(DotName annotation, BeanInfo bean,
            MethodInfo method) {
        if (method.returnType().kind() == Type.Kind.VOID) {
            String error = annotation + " annotation is not allowed on a method returning void";
            return Optional.of(new IllegalReturnTypeException(buildExceptionMessage(error, bean.getBeanClass(), method)));
        }
        return Optional.empty();
    }

    private static String buildExceptionMessage(String error, DotName beanClass, MethodInfo method) {
        return error + ": [class: " + beanClass + ", method: " + method + "]";
    }
}
