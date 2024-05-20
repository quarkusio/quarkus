package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.DisposerInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;

class DisposerInfoImpl implements DisposerInfo {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final io.quarkus.arc.processor.DisposerInfo arcDisposerInfo;

    DisposerInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.DisposerInfo arcDisposerInfo) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.arcDisposerInfo = arcDisposerInfo;
    }

    @Override
    public MethodInfo disposerMethod() {
        org.jboss.jandex.MethodInfo jandexMethod = arcDisposerInfo.getDisposerMethod();
        return new MethodInfoImpl(jandexIndex, annotationOverlay, jandexMethod);
    }

    @Override
    public ParameterInfo disposedParameter() {
        org.jboss.jandex.MethodParameterInfo jandexParameter = arcDisposerInfo.getDisposedParameter();
        return new ParameterInfoImpl(jandexIndex, annotationOverlay, jandexParameter);
    }
}
