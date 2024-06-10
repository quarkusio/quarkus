package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.hasStandardSecurityAnnotation;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class InterfaceMethodLevelAnnotationsTransformer implements AnnotationsTransformer {

    private final IndexView index;

    public InterfaceMethodLevelAnnotationsTransformer(IndexView index) {
        this.index = index;
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.METHOD;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        MethodInfo methodInfo = transformationContext.getTarget().asMethod();
        if (hasStandardSecurityAnnotation(methodInfo)) {
            return;
        }
        MethodInfo interfaceMethodInfo = findAnnotatedInterfaceMethod(methodInfo);
        if (interfaceMethodInfo != null) {
            transformationContext.transform().addAll(interfaceMethodInfo.annotations()).done();
        }
    }

    private MethodInfo findAnnotatedInterfaceMethod(MethodInfo methodInfo) {
        for (DotName name : methodInfo.declaringClass().interfaceNames()) {
            ClassInfo classByName = index.getClassByName(name);
            MethodInfo method = classByName.method(methodInfo.name(), methodInfo.parameterTypes().toArray(new Type[0]));
            if (method != null && hasStandardSecurityAnnotation(method)) {
                return method;
            }
        }
        return null;
    }
}
