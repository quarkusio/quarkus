package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;

import jakarta.enterprise.inject.build.compatible.spi.InjectionPointInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.DeclarationInfo;
import jakarta.enterprise.lang.model.types.Type;

class InjectionPointInfoImpl implements InjectionPointInfo {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final io.quarkus.arc.processor.InjectionPointInfo arcInjectionPointInfo;

    InjectionPointInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.InjectionPointInfo arcInjectionPointInfo) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.arcInjectionPointInfo = arcInjectionPointInfo;
    }

    @Override
    public Type type() {
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, arcInjectionPointInfo.getRequiredType());
    }

    @Override
    public Collection<AnnotationInfo> qualifiers() {
        return arcInjectionPointInfo.getRequiredQualifiers()
                .stream()
                .map(it -> (AnnotationInfo) new AnnotationInfoImpl(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public DeclarationInfo declaration() {
        if (arcInjectionPointInfo.isField()) {
            org.jboss.jandex.FieldInfo jandexField = arcInjectionPointInfo.getAnnotationTarget().asField();
            return new FieldInfoImpl(jandexIndex, annotationOverlay, jandexField);
        } else if (arcInjectionPointInfo.isParam()) {
            return new ParameterInfoImpl(jandexIndex, annotationOverlay,
                    arcInjectionPointInfo.getAnnotationTarget().asMethodParameter());
        } else {
            throw new IllegalStateException("Unknown injection point: " + arcInjectionPointInfo);
        }
    }
}
