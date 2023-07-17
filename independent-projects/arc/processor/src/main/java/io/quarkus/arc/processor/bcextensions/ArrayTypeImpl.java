package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.types.ArrayType;
import jakarta.enterprise.lang.model.types.Type;

class ArrayTypeImpl extends TypeImpl<org.jboss.jandex.ArrayType> implements ArrayType {
    ArrayTypeImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.ArrayType jandexType) {
        super(jandexIndex, annotationOverlays, jandexType);
    }

    @Override
    public Type componentType() {
        int dimensions = jandexType.dimensions();
        org.jboss.jandex.Type componentType = dimensions == 1
                ? jandexType.constituent()
                : org.jboss.jandex.ArrayType.create(jandexType.constituent(), dimensions - 1);
        return fromJandexType(jandexIndex, annotationOverlays, componentType);
    }
}
