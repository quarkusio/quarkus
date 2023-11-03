package io.quarkus.hibernate.validator.deployment;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.hibernate.validator.runtime.interceptor.MethodValidated;
import io.quarkus.hibernate.validator.runtime.jaxrs.JaxrsEndPointValidated;

/**
 * Add {@link MethodValidated} annotations to the methods requiring validation.
 */
public class MethodValidatedAnnotationsTransformer implements AnnotationsTransformer {

    private static final Logger LOGGER = Logger.getLogger(MethodValidatedAnnotationsTransformer.class.getPackage().getName());

    private final Set<DotName> consideredAnnotations;
    private final Map<DotName, Set<SimpleMethodSignatureKey>> methodsWithInheritedValidation;
    private final Map<DotName, Set<SimpleMethodSignatureKey>> jaxRsMethods;

    MethodValidatedAnnotationsTransformer(Set<DotName> consideredAnnotations,
            Map<DotName, Set<SimpleMethodSignatureKey>> jaxRsMethods,
            Map<DotName, Set<SimpleMethodSignatureKey>> methodsWithInheritedValidation) {
        this.consideredAnnotations = consideredAnnotations;
        this.jaxRsMethods = jaxRsMethods;
        this.methodsWithInheritedValidation = methodsWithInheritedValidation;
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
                return !isSynthetic(method.flags());
            }
        }

        // This method has no annotations of its own: look for inherited annotations

        Set<SimpleMethodSignatureKey> validatedMethods = methodsWithInheritedValidation.get(method.declaringClass().name());
        if (validatedMethods == null || validatedMethods.isEmpty()) {
            return false;
        }

        return validatedMethods.contains(new SimpleMethodSignatureKey(method));
    }

    private boolean isSynthetic(int mod) {
        return (mod & Opcodes.ACC_SYNTHETIC) != 0;
    }

    private boolean isJaxrsMethod(MethodInfo method) {
        ClassInfo clazz = method.declaringClass();
        SimpleMethodSignatureKey signatureKey = new SimpleMethodSignatureKey(method);

        if (isJaxrsMethod(signatureKey, clazz.name())) {
            return true;
        }

        // check interfaces
        for (DotName iface : clazz.interfaceNames()) {
            if (isJaxrsMethod(signatureKey, iface)) {
                return true;
            }
        }

        // check superclass, but only the direct one since we do not (yet) have the entire ClassInfo hierarchy here
        DotName superClass = clazz.superName();
        if (!superClass.equals(IndexingUtil.OBJECT)) {
            if (isJaxrsMethod(signatureKey, superClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJaxrsMethod(SimpleMethodSignatureKey signatureKey, DotName dotName) {
        Set<SimpleMethodSignatureKey> signatureKeys = jaxRsMethods.get(dotName);
        return signatureKeys != null && signatureKeys.contains(signatureKey);
    }
}
