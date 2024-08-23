package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.WildcardType;

class WildcardTypeImpl extends TypeImpl<org.jboss.jandex.WildcardType> implements WildcardType {
    private final boolean hasUpperBound;

    WildcardTypeImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.WildcardType jandexType) {
        super(jandexIndex, annotationOverlay, jandexType);
        this.hasUpperBound = jandexType.superBound() == null;
    }

    @Override
    public Type upperBound() {
        if (!hasUpperBound) {
            return null;
        }
        return fromJandexType(jandexIndex, annotationOverlay, jandexType.extendsBound());
    }

    @Override
    public Type lowerBound() {
        if (hasUpperBound) {
            return null;
        }
        return fromJandexType(jandexIndex, annotationOverlay, jandexType.superBound());
    }
}
