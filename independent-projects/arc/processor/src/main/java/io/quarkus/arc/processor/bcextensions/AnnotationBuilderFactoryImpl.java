package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilder;
import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilderFactory;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import org.jboss.jandex.DotName;

final class AnnotationBuilderFactoryImpl implements AnnotationBuilderFactory {
    private final org.jboss.jandex.IndexView beanArchiveIndex;
    private final AllAnnotationOverlays annotationOverlays;

    AnnotationBuilderFactoryImpl(org.jboss.jandex.IndexView beanArchiveIndex, AllAnnotationOverlays annotationOverlays) {
        this.beanArchiveIndex = beanArchiveIndex;
        this.annotationOverlays = annotationOverlays;
    }

    @Override
    public AnnotationBuilder create(Class<? extends Annotation> annotationType) {
        if (beanArchiveIndex == null || annotationOverlays == null) {
            throw new IllegalStateException("Can't create AnnotationBuilder right now");
        }

        DotName jandexAnnotationName = DotName.createSimple(annotationType.getName());
        return new AnnotationBuilderImpl(beanArchiveIndex, annotationOverlays, jandexAnnotationName);
    }

    @Override
    public AnnotationBuilder create(ClassInfo annotationType) {
        if (beanArchiveIndex == null || annotationOverlays == null) {
            throw new IllegalStateException("Can't create AnnotationBuilder right now");
        }

        DotName jandexAnnotationName = ((ClassInfoImpl) annotationType).jandexDeclaration.name();
        return new AnnotationBuilderImpl(beanArchiveIndex, annotationOverlays, jandexAnnotationName);
    }
}
