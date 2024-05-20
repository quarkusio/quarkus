package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;

import jakarta.enterprise.inject.build.compatible.spi.ScopeInfo;
import jakarta.enterprise.inject.build.compatible.spi.StereotypeInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;

class StereotypeInfoImpl implements StereotypeInfo {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final io.quarkus.arc.processor.StereotypeInfo arcStereotype;

    StereotypeInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.StereotypeInfo arcStereotype) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.arcStereotype = arcStereotype;
    }

    @Override
    public ScopeInfo defaultScope() {
        return new ScopeInfoImpl(jandexIndex, annotationOverlay, arcStereotype.getDefaultScope());
    }

    @Override
    public Collection<AnnotationInfo> interceptorBindings() {
        return arcStereotype.getInterceptorBindings()
                .stream()
                .map(it -> (AnnotationInfo) new AnnotationInfoImpl(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public boolean isAlternative() {
        return arcStereotype.isAlternative();
    }

    @Override
    public Integer priority() {
        return arcStereotype.getAlternativePriority();
    }

    @Override
    public boolean isNamed() {
        return arcStereotype.isNamed();
    }
}
