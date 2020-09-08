package io.quarkus.hibernate.validator.deployment;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

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
    private final Map<DotName, Set<SimpleMethodSignatureKey>> inheritedAnnotationsToBeValidated;
    private final Map<DotName, Set<SimpleMethodSignatureKey>> jaxRsMethods;

    MethodValidatedAnnotationsTransformer(Set<DotName> consideredAnnotations,
            Map<DotName, Set<SimpleMethodSignatureKey>> jaxRsMethods,
            Map<DotName, Set<SimpleMethodSignatureKey>> inheritedAnnotationsToBeValidated) {
        this.consideredAnnotations = consideredAnnotations;
        this.jaxRsMethods = jaxRsMethods;
        this.inheritedAnnotationsToBeValidated = inheritedAnnotationsToBeValidated;
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
        SimpleMethodSignatureKey signatureKey = new SimpleMethodSignatureKey(method);
        for (Map.Entry<DotName, Set<SimpleMethodSignatureKey>> validatedMethod : inheritedAnnotationsToBeValidated.entrySet()) {
            DotName ifaceOrSuperClass = validatedMethod.getKey();
            // note: only check the direct superclass since we do not (yet) have the entire ClassInfo hierarchy here
            if ((clazz.interfaceNames().contains(ifaceOrSuperClass) || ifaceOrSuperClass.equals(clazz.superName()))
                    && validatedMethod.getValue().contains(signatureKey)) {
                return true;
            }
        }
        return false;
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
