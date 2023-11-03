package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.PackageInfo;

class PackageInfoImpl implements PackageInfo {
    final org.jboss.jandex.IndexView jandexIndex;
    final AllAnnotationOverlays annotationOverlays;
    final org.jboss.jandex.ClassInfo jandexDeclaration; // package-info.class

    private AnnotationSet annotationSet;

    PackageInfoImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.ClassInfo jandexDeclaration) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlays = annotationOverlays;
        this.jandexDeclaration = jandexDeclaration;
    }

    @Override
    public String name() {
        return jandexDeclaration.name().packagePrefix();
    }

    private AnnotationSet annotationSet() {
        if (annotationSet == null) {
            annotationSet = new AnnotationSet(jandexDeclaration.declaredAnnotations());
        }

        return annotationSet;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return annotationSet().hasAnnotation(annotationType);
    }

    @Override
    public boolean hasAnnotation(Predicate<AnnotationInfo> predicate) {
        return annotationSet().annotations()
                .stream()
                .anyMatch(it -> predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlays, it)));
    }

    @Override
    public <T extends Annotation> AnnotationInfo annotation(Class<T> annotationType) {
        org.jboss.jandex.AnnotationInstance jandexAnnotation = annotationSet().annotation(annotationType);
        if (jandexAnnotation == null) {
            return null;
        }
        return new AnnotationInfoImpl(jandexIndex, annotationOverlays, jandexAnnotation);
    }

    @Override
    public <T extends Annotation> Collection<AnnotationInfo> repeatableAnnotation(Class<T> annotationType) {
        return annotationSet().annotationsWithRepeatable(annotationType)
                .stream()
                .map(it -> new AnnotationInfoImpl(jandexIndex, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<AnnotationInfo> annotations(Predicate<AnnotationInfo> predicate) {
        return annotationSet().annotations()
                .stream()
                .map(it -> new AnnotationInfoImpl(jandexIndex, annotationOverlays, it))
                .filter(predicate)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<AnnotationInfo> annotations() {
        return annotations(it -> true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PackageInfoImpl that = (PackageInfoImpl) o;
        return Objects.equals(jandexDeclaration.name(), that.jandexDeclaration.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(jandexDeclaration.name());
    }
}
