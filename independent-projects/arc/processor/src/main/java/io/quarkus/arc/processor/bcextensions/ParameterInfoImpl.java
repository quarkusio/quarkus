package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.jandex.DotName;

class ParameterInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.MethodParameterInfo> implements ParameterInfo {
    ParameterInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.MethodParameterInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public String name() {
        String name = jandexDeclaration.name();
        return name != null ? name : "arg" + jandexDeclaration.position();
    }

    @Override
    public Type type() {
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, jandexDeclaration.type());
    }

    @Override
    public MethodInfo declaringMethod() {
        return new MethodInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration.method());
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        DotName annotationName = DotName.createSimple(annotationType);
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration.method())) {
            if (annotation.name().equals(annotationName)
                    && annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                    && annotation.target().asMethodParameter().position() == jandexDeclaration.position()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnnotation(Predicate<AnnotationInfo> predicate) {
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration.method())) {
            if (predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation))
                    && annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                    && annotation.target().asMethodParameter().position() == jandexDeclaration.position()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Annotation> AnnotationInfo annotation(Class<T> annotationType) {
        DotName annotationName = DotName.createSimple(annotationType);
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration.method())) {
            if (annotation.name().equals(annotationName)
                    && annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                    && annotation.target().asMethodParameter().position() == jandexDeclaration.position()) {
                return new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation);
            }
        }
        return null;
    }

    @Override
    public <T extends Annotation> Collection<AnnotationInfo> repeatableAnnotation(Class<T> annotationType) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotationsWithRepeatable(
                jandexDeclaration.method(), annotationType)) {
            if (annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                    && annotation.target().asMethodParameter().position() == jandexDeclaration.position()) {
                result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations(Predicate<AnnotationInfo> predicate) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration.method())) {
            if (annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                    && annotation.target().asMethodParameter().position() == jandexDeclaration.position()) {
                AnnotationInfo annotationInfo = new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation);
                if (predicate.test(annotationInfo)) {
                    result.add(annotationInfo);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations() {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration.method())) {
            if (annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER
                    && annotation.target().asMethodParameter().position() == jandexDeclaration.position()) {
                result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String toString() {
        return "parameter " + name() + " of method " + jandexDeclaration.method();
    }
}
