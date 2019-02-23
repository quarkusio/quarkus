package io.quarkus.hibernate.validator;

import java.util.Set;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.quarkus.arc.processor.AnnotationsTransformer;

import io.quarkus.hibernate.validator.runtime.interceptor.MethodValidated;
import io.quarkus.hibernate.validator.runtime.jaxrs.JaxrsEndPointValidated;

/**
 * Add {@link MethodValidated} annotations to the methods requiring validation.
 */
public class MethodValidatedAnnotationsTransformer implements AnnotationsTransformer {

    private static final DotName[] JAXRS_METHOD_ANNOTATIONS = {
            DotName.createSimple("javax.ws.rs.GET"),
            DotName.createSimple("javax.ws.rs.HEAD"),
            DotName.createSimple("javax.ws.rs.DELETE"),
            DotName.createSimple("javax.ws.rs.OPTIONS"),
            DotName.createSimple("javax.ws.rs.PATCH"),
            DotName.createSimple("javax.ws.rs.POST"),
            DotName.createSimple("javax.ws.rs.PUT"),
    };

    private final Set<DotName> consideredAnnotations;

    MethodValidatedAnnotationsTransformer(Set<DotName> consideredAnnotations) {
        this.consideredAnnotations = consideredAnnotations;
    }

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.METHOD == kind;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        MethodInfo method = transformationContext.getTarget().asMethod();

        if (requiresValidation(method)) {
            if (isJaxrsMethod(method)) {
                transformationContext.transform().add(DotName.createSimple(JaxrsEndPointValidated.class.getName())).done();
            } else {
                transformationContext.transform().add(DotName.createSimple(MethodValidated.class.getName())).done();
            }
        }
    }

    private boolean requiresValidation(MethodInfo method) {
        if (method.annotations().isEmpty()) {
            return false;
        }

        for (DotName consideredAnnotation : consideredAnnotations) {
            if (method.hasAnnotation(consideredAnnotation)) {
                return true;
            }
        }

        return false;
    }

    private boolean isJaxrsMethod(MethodInfo method) {
        for (DotName jaxrsMethodAnnotation : JAXRS_METHOD_ANNOTATIONS) {
            if (method.hasAnnotation(jaxrsMethodAnnotation)) {
                return true;
            }
        }
        return false;
    }
}
