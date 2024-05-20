package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.DeclarationInfo;

abstract class DeclarationInfoImpl<JandexDeclaration extends org.jboss.jandex.Declaration> extends AnnotationTargetImpl
        implements DeclarationInfo {
    final JandexDeclaration jandexDeclaration;

    DeclarationInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            JandexDeclaration jandexDeclaration) {
        super(jandexIndex, annotationOverlay, org.jboss.jandex.EquivalenceKey.of(jandexDeclaration));
        this.jandexDeclaration = jandexDeclaration;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return annotationOverlay.hasAnnotation(jandexDeclaration, annotationType);
    }

    @Override
    public boolean hasAnnotation(Predicate<AnnotationInfo> predicate) {
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            if (predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Annotation> AnnotationInfo annotation(Class<T> annotationType) {
        org.jboss.jandex.AnnotationInstance annotation = annotationOverlay.annotation(jandexDeclaration, annotationType);
        if (annotation == null) {
            return null;
        }
        return new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation);
    }

    @Override
    public <T extends Annotation> Collection<AnnotationInfo> repeatableAnnotation(Class<T> annotationType) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotationsWithRepeatable(jandexDeclaration,
                annotationType)) {
            result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations(Predicate<AnnotationInfo> predicate) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            AnnotationInfo annotationInfo = new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation);
            if (predicate.test(annotationInfo)) {
                result.add(annotationInfo);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations() {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String toString() {
        return jandexDeclaration.toString();
    }
}
