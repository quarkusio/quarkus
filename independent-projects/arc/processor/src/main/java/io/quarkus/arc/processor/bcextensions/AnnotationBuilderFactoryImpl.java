package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilder;
import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilderFactory;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import org.jboss.jandex.DotName;

final class AnnotationBuilderFactoryImpl implements AnnotationBuilderFactory {
    private final org.jboss.jandex.IndexView beanArchiveIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    AnnotationBuilderFactoryImpl(org.jboss.jandex.IndexView beanArchiveIndex,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay) {
        this.beanArchiveIndex = beanArchiveIndex;
        this.annotationOverlay = annotationOverlay;
    }

    @Override
    public AnnotationBuilder create(Class<? extends Annotation> annotationType) {
        if (beanArchiveIndex == null || annotationOverlay == null) {
            throw new IllegalStateException("Can't create AnnotationBuilder right now");
        }

        DotName jandexAnnotationName = DotName.createSimple(annotationType.getName());
        return new AnnotationBuilderImpl(beanArchiveIndex, annotationOverlay, jandexAnnotationName);
    }

    @Override
    public AnnotationBuilder create(ClassInfo annotationType) {
        if (beanArchiveIndex == null || annotationOverlay == null) {
            throw new IllegalStateException("Can't create AnnotationBuilder right now");
        }

        DotName jandexAnnotationName = ((ClassInfoImpl) annotationType).jandexDeclaration.name();
        return new AnnotationBuilderImpl(beanArchiveIndex, annotationOverlay, jandexAnnotationName);
    }
}
