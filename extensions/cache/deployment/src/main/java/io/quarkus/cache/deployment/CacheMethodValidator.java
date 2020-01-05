package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.cache.deployment.exception.IllegalReturnTypeException;

public class CacheMethodValidator {

    public static void validateAnnotations(AnnotationStore annotationStore, BeanInfo bean, MethodInfo method,
            List<Throwable> throwables) {

        AnnotationInstance cacheResult = annotationStore.getAnnotation(method, CACHE_RESULT);
        if (cacheResult != null && method.returnType().kind() == Kind.VOID) {
            String exceptionMessage = "The @CacheResult annotation is not allowed on a method returning void: [class= "
                    + bean.getBeanClass() + ", method= " + method + "]";
            throwables.add(new IllegalReturnTypeException(exceptionMessage));
        }
    }
}
