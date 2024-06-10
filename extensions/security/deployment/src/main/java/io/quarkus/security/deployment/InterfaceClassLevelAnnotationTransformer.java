package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.hasStandardSecurityAnnotation;

import org.jboss.jandex.*;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class InterfaceClassLevelAnnotationTransformer implements AnnotationsTransformer {

    private final IndexView index;

    public InterfaceClassLevelAnnotationTransformer(IndexView index) {
        this.index = index;
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        ClassInfo classInfo = transformationContext.getTarget().asClass();
        if (hasStandardSecurityAnnotation(classInfo)) {
            return;
        }
        ClassInfo interfaceClassInfo = findAnnotatedInterfaceMethod(classInfo);
        if (interfaceClassInfo != null) {
            transformationContext.transform().addAll(interfaceClassInfo.annotations()).done();
        }
    }

    private ClassInfo findAnnotatedInterfaceMethod(ClassInfo classInfo) {
        for (DotName name : classInfo.interfaceNames()) {
            ClassInfo classByName = index.getClassByName(name);
            if (classByName != null && hasStandardSecurityAnnotation(classByName)) {
                return classByName;
            }
        }
        return null;
    }
}
