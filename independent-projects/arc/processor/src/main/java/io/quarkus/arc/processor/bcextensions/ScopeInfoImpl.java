package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.ScopeInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

class ScopeInfoImpl implements ScopeInfo {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final AllAnnotationOverlays annotationOverlays;
    private final io.quarkus.arc.processor.ScopeInfo arcScopeInfo;

    ScopeInfoImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            io.quarkus.arc.processor.ScopeInfo arcScopeInfo) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlays = annotationOverlays;
        this.arcScopeInfo = arcScopeInfo;
    }

    @Override
    public ClassInfo annotation() {
        org.jboss.jandex.ClassInfo jandexClass = jandexIndex.getClassByName(arcScopeInfo.getDotName());
        return new ClassInfoImpl(jandexIndex, annotationOverlays, jandexClass);
    }

    @Override
    public boolean isNormal() {
        return arcScopeInfo.isNormal();
    }
}
