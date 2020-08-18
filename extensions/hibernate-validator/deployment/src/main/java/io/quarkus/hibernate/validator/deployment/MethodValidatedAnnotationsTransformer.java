package io.quarkus.hibernate.validator.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.hibernate.validator.runtime.interceptor.MethodValidated;
import io.quarkus.hibernate.validator.runtime.jaxrs.JaxrsEndPointValidated;

/**
 * Add {@link MethodValidated} annotations to the methods requiring validation.
 */
public class MethodValidatedAnnotationsTransformer implements AnnotationsTransformer {

    private static final Logger LOGGER = Logger.getLogger(MethodValidatedAnnotationsTransformer.class.getPackage().getName());

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
    private final Collection<DotName> effectiveJaxRsMethodDefiningAnnotations;
    private final Map<DotName, Set<String>> inheritedAnnotationsToBeValidated;

    MethodValidatedAnnotationsTransformer(Set<DotName> consideredAnnotations,
            Collection<DotName> additionalJaxRsMethodAnnotationsDotNames,
            Map<DotName, Set<String>> inheritedAnnotationsToBeValidated) {
        this.consideredAnnotations = consideredAnnotations;
        this.inheritedAnnotationsToBeValidated = inheritedAnnotationsToBeValidated;

        this.effectiveJaxRsMethodDefiningAnnotations = new ArrayList<>(
                JAXRS_METHOD_ANNOTATIONS.length + additionalJaxRsMethodAnnotationsDotNames.size());
        effectiveJaxRsMethodDefiningAnnotations.addAll(Arrays.asList(JAXRS_METHOD_ANNOTATIONS));
        effectiveJaxRsMethodDefiningAnnotations.addAll(additionalJaxRsMethodAnnotationsDotNames);
    }

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.METHOD == kind;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        MethodInfo method = transformationContext.getTarget().asMethod();

        if (requiresValidation(method)) {
            if (Modifier.isStatic(method.flags())) {
                // We don't support validating methods on static methods yet as it used to not be supported by CDI/Weld
                // Supporting it will require some work in Hibernate Validator so we are going back to the old behavior of ignoring them but we log a warning.
                LOGGER.warnf(
                        "Hibernate Validator does not support constraints on static methods yet. Constraints on %s are ignored.",
                        method.declaringClass().name().toString() + "#" + method.toString());
                return;
            }

            if (isJaxrsMethod(method)) {
                transformationContext.transform().add(DotName.createSimple(JaxrsEndPointValidated.class.getName())).done();
            } else {
                transformationContext.transform().add(DotName.createSimple(MethodValidated.class.getName())).done();
            }
        }
    }

    private boolean requiresValidation(MethodInfo method) {
        for (DotName consideredAnnotation : consideredAnnotations) {
            if (method.hasAnnotation(consideredAnnotation)) {
                return true;
            }
        }
        // This method has no annotations of its own: look for inherited annotations
        ClassInfo clazz = method.declaringClass();
        String methodName = method.name().toString();
        for (Map.Entry<DotName, Set<String>> validatedMethod : inheritedAnnotationsToBeValidated.entrySet()) {
            if (clazz.interfaceNames().contains(validatedMethod.getKey())
                    && validatedMethod.getValue().contains(methodName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJaxrsMethod(MethodInfo method) {
        for (DotName jaxrsMethodAnnotation : effectiveJaxRsMethodDefiningAnnotations) {
            if (method.hasAnnotation(jaxrsMethodAnnotation)) {
                return true;
            }
        }
        return false;
    }
}
