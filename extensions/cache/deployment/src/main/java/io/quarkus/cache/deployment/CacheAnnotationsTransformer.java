package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_LIST;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_KEY;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_KEY_PARAMETER_POSITIONS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;
import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;
import static org.jboss.jandex.AnnotationValue.createArrayValue;
import static org.jboss.jandex.AnnotationValue.createShortValue;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class CacheAnnotationsTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(Kind kind) {
        return kind == METHOD;
    }

    @Override
    public void transform(TransformationContext context) {
        MethodInfo method = context.getTarget().asMethod();
        if (requiresCacheKeyParameterPositionsInterceptorBinding(method)) {
            List<AnnotationValue> positions = new ArrayList<>();
            for (AnnotationInstance annotation : method.annotations(CACHE_KEY)) {
                positions.add(createShortValue("", annotation.target().asMethodParameter().position()));
            }
            if (!positions.isEmpty()) {
                AnnotationValue annotationValue = createArrayValue("value", toArray(positions));
                AnnotationInstance binding = create(CACHE_KEY_PARAMETER_POSITIONS, method,
                        new AnnotationValue[] { annotationValue });
                context.transform().add(binding).done();
            }
        }
    }

    private boolean requiresCacheKeyParameterPositionsInterceptorBinding(MethodInfo method) {
        return method.hasAnnotation(CACHE_KEY) && (method.hasAnnotation(CACHE_INVALIDATE)
                || method.hasAnnotation(CACHE_INVALIDATE_LIST) || method.hasAnnotation(CACHE_RESULT));
    }

    private AnnotationValue[] toArray(List<AnnotationValue> parameters) {
        return parameters.toArray(new AnnotationValue[0]);
    }
}
