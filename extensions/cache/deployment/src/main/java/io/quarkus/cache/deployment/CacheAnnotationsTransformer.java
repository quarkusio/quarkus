package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_ALL;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_ALL_LIST;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_LIST;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_KEY;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_KEY_PARAMETER_POSITIONS_PARAM;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAM;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.LOCK_TIMEOUT_PARAM;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.cache.runtime.CacheInvalidateAllInterceptorBinding;
import io.quarkus.cache.runtime.CacheInvalidateInterceptorBinding;
import io.quarkus.cache.runtime.CacheResultInterceptorBinding;

public class CacheAnnotationsTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.METHOD == kind;
    }

    @Override
    public void transform(TransformationContext context) {
        MethodInfo method = context.getTarget().asMethod();
        List<AnnotationInstance> interceptorBindings = new ArrayList<>();
        for (AnnotationInstance annotation : method.annotations()) {
            AnnotationTarget target = annotation.target();
            if (target.kind() == Kind.METHOD) {
                if (CACHE_INVALIDATE_ALL.equals(annotation.name())) {
                    interceptorBindings.add(createCacheInvalidateAllBinding(annotation, target));
                } else if (CACHE_INVALIDATE_ALL_LIST.equals(annotation.name())) {
                    for (AnnotationInstance nestedAnnotation : annotation.value("value").asNestedArray()) {
                        interceptorBindings.add(createCacheInvalidateAllBinding(nestedAnnotation, target));
                    }
                } else if (CACHE_INVALIDATE.equals(annotation.name())) {
                    interceptorBindings.add(createCacheInvalidateBinding(method, annotation, target));
                } else if (CACHE_INVALIDATE_LIST.equals(annotation.name())) {
                    for (AnnotationInstance nestedAnnotation : annotation.value("value").asNestedArray()) {
                        interceptorBindings.add(createCacheInvalidateBinding(method, nestedAnnotation, target));
                    }
                } else if (CACHE_RESULT.equals(annotation.name())) {
                    interceptorBindings.add(createCacheResultBinding(method, annotation, target));
                }
            }
        }
        context.transform().addAll(interceptorBindings).done();
    }

    private AnnotationInstance createCacheInvalidateAllBinding(AnnotationInstance annotation, AnnotationTarget target) {
        return createBinding(CacheInvalidateAllInterceptorBinding.class, target, getCacheName(annotation));
    }

    private AnnotationInstance createCacheInvalidateBinding(MethodInfo method, AnnotationInstance annotation,
            AnnotationTarget target) {
        List<AnnotationValue> parameters = new ArrayList<>();
        parameters.add(getCacheName(annotation));
        findCacheKeyParameters(method).ifPresent(parameters::add);
        return createBinding(CacheInvalidateInterceptorBinding.class, target, toArray(parameters));
    }

    private AnnotationInstance createCacheResultBinding(MethodInfo method, AnnotationInstance annotation,
            AnnotationTarget target) {
        List<AnnotationValue> parameters = new ArrayList<>();
        parameters.add(getCacheName(annotation));
        findCacheKeyParameters(method).ifPresent(parameters::add);
        findLockTimeout(annotation).ifPresent(parameters::add);
        return createBinding(CacheResultInterceptorBinding.class, target, toArray(parameters));
    }

    private AnnotationInstance createBinding(Class<?> bindingClass, AnnotationTarget target, AnnotationValue... values) {
        return AnnotationInstance.create(DotName.createSimple(bindingClass.getName()), target, values);
    }

    private AnnotationValue getCacheName(AnnotationInstance annotation) {
        return annotation.value(CACHE_NAME_PARAM);
    }

    private Optional<AnnotationValue> findCacheKeyParameters(MethodInfo method) {
        List<AnnotationValue> parameters = new ArrayList<>();
        for (AnnotationInstance annotation : method.annotations()) {
            if (annotation.target().kind() == Kind.METHOD_PARAMETER && CACHE_KEY.equals(annotation.name())) {
                parameters.add(AnnotationValue.createShortValue("", annotation.target().asMethodParameter().position()));
            }
        }
        if (parameters.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AnnotationValue.createArrayValue(CACHE_KEY_PARAMETER_POSITIONS_PARAM, toArray(parameters)));
    }

    private Optional<AnnotationValue> findLockTimeout(AnnotationInstance annotation) {
        return Optional.ofNullable(annotation.value(LOCK_TIMEOUT_PARAM));
    }

    private AnnotationValue[] toArray(List<AnnotationValue> parameters) {
        return parameters.toArray(new AnnotationValue[0]);
    }
}
