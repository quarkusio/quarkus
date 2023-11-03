package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilderFactory;
import jakarta.enterprise.inject.build.compatible.spi.BuildServices;

public class BuildServicesImpl implements BuildServices {
    private static volatile org.jboss.jandex.IndexView beanArchiveIndex;
    private static volatile AllAnnotationOverlays annotationOverlays;

    static void init(org.jboss.jandex.IndexView beanArchiveIndex, AllAnnotationOverlays annotationOverlays) {
        BuildServicesImpl.beanArchiveIndex = beanArchiveIndex;
        BuildServicesImpl.annotationOverlays = annotationOverlays;
    }

    static void reset() {
        BuildServicesImpl.beanArchiveIndex = null;
        BuildServicesImpl.annotationOverlays = null;
    }

    @Override
    public AnnotationBuilderFactory annotationBuilderFactory() {
        return new AnnotationBuilderFactoryImpl(beanArchiveIndex, annotationOverlays);
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
