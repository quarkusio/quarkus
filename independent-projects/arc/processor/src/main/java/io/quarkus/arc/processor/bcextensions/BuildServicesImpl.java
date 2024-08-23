package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilderFactory;
import jakarta.enterprise.inject.build.compatible.spi.BuildServices;

public class BuildServicesImpl implements BuildServices {
    private static volatile org.jboss.jandex.IndexView beanArchiveIndex;
    private static volatile org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    static void init(org.jboss.jandex.IndexView beanArchiveIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay) {
        BuildServicesImpl.beanArchiveIndex = beanArchiveIndex;
        BuildServicesImpl.annotationOverlay = annotationOverlay;
    }

    static void reset() {
        BuildServicesImpl.beanArchiveIndex = null;
        BuildServicesImpl.annotationOverlay = null;
    }

    @Override
    public AnnotationBuilderFactory annotationBuilderFactory() {
        return new AnnotationBuilderFactoryImpl(beanArchiveIndex, annotationOverlay);
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
