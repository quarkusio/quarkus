package io.quarkus.resteasy.deployment;

import static io.quarkus.security.spi.SecurityTransformerUtils.DENY_ALL;
import static io.quarkus.security.spi.SecurityTransformerUtils.hasSecurityAnnotation;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.arc.processor.AnnotationsTransformer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyJaxRsTransformer implements AnnotationsTransformer {
    private final ResteasyDeployment resteasyDeployment;

    public DenyJaxRsTransformer(ResteasyDeployment resteasyDeployment) {
        this.resteasyDeployment = resteasyDeployment;
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        ClassInfo classInfo = transformationContext.getTarget().asClass();
        if (requiresSyntheticDenyAll(classInfo)) {
            transformationContext.transform().add(DENY_ALL).done();
        }
    }

    public boolean requiresSyntheticDenyAll(ClassInfo classInfo) {
        return !hasSecurityAnnotation(classInfo) && isJaxRsResource(classInfo);
    }

    private boolean isJaxRsResource(ClassInfo classInfo) {
        String className = classInfo.name().toString();
        return resteasyDeployment.getScannedResourceClasses().contains(className);
    }
}
