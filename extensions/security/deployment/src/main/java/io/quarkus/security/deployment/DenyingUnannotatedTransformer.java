package io.quarkus.security.deployment;

import static io.quarkus.security.spi.SecurityTransformerUtils.DENY_ALL;

import java.util.List;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.security.spi.SecurityTransformerUtils;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyingUnannotatedTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        ClassInfo classInfo = transformationContext.getTarget().asClass();
        List<MethodInfo> methods = classInfo.methods();
        if (!SecurityTransformerUtils.hasSecurityAnnotation(classInfo)
                && methods.stream().anyMatch(SecurityTransformerUtils::hasSecurityAnnotation)) {
            transformationContext.transform().add(DENY_ALL).done();
        }
    }
}
