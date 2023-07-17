package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import jakarta.enterprise.inject.build.compatible.spi.DeclarationConfig;
import jakarta.enterprise.lang.model.AnnotationInfo;

abstract class DeclarationConfigImpl<JandexDeclaration extends org.jboss.jandex.AnnotationTarget, THIS extends DeclarationConfigImpl<JandexDeclaration, THIS>>
        implements DeclarationConfig {
    final org.jboss.jandex.IndexView jandexIndex;
    final AllAnnotationTransformations allTransformations;
    final AnnotationsTransformation<JandexDeclaration> transformations;
    final JandexDeclaration jandexDeclaration;

    DeclarationConfigImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationTransformations allTransformations,
            AnnotationsTransformation<JandexDeclaration> transformations, JandexDeclaration jandexDeclaration) {
        this.jandexIndex = jandexIndex;
        this.allTransformations = allTransformations;
        this.transformations = transformations;
        this.jandexDeclaration = jandexDeclaration;
    }

    @Override
    public THIS addAnnotation(Class<? extends Annotation> annotationType) {
        transformations.addAnnotation(jandexDeclaration, annotationType);
        return (THIS) this;
    }

    @Override
    public THIS addAnnotation(AnnotationInfo annotation) {
        transformations.addAnnotation(jandexDeclaration, annotation);
        return (THIS) this;
    }

    @Override
    public THIS addAnnotation(Annotation annotation) {
        transformations.addAnnotation(jandexDeclaration, annotation);
        return (THIS) this;
    }

    @Override
    public THIS removeAnnotation(Predicate<AnnotationInfo> predicate) {
        transformations.removeAnnotation(jandexDeclaration, predicate);
        return (THIS) this;
    }

    @Override
    public THIS removeAllAnnotations() {
        transformations.removeAllAnnotations(jandexDeclaration);
        return (THIS) this;
    }
}
