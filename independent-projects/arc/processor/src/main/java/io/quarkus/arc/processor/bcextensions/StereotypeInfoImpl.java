package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.build.compatible.spi.ScopeInfo;
import jakarta.enterprise.inject.build.compatible.spi.StereotypeInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;

class StereotypeInfoImpl implements StereotypeInfo {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final AllAnnotationOverlays annotationOverlays;
    private final io.quarkus.arc.processor.StereotypeInfo arcStereotype;

    StereotypeInfoImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            io.quarkus.arc.processor.StereotypeInfo arcStereotype) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlays = annotationOverlays;
        this.arcStereotype = arcStereotype;
    }

    @Override
    public ScopeInfo defaultScope() {
        return new ScopeInfoImpl(jandexIndex, annotationOverlays, arcStereotype.getDefaultScope());
    }

    @Override
    public Collection<AnnotationInfo> interceptorBindings() {
        return arcStereotype.getInterceptorBindings()
                .stream()
                .map(it -> new AnnotationInfoImpl(jandexIndex, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
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
