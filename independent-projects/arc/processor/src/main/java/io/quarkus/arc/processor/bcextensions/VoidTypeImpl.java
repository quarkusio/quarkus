package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.types.VoidType;

class VoidTypeImpl extends TypeImpl<org.jboss.jandex.VoidType> implements VoidType {
    VoidTypeImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.VoidType jandexType) {
        super(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public String name() {
        return jandexType.name().toString();
    }
}
