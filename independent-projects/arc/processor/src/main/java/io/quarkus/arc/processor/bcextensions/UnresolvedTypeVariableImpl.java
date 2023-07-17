package io.quarkus.arc.processor.bcextensions;

import java.util.List;

import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.TypeVariable;

class UnresolvedTypeVariableImpl extends TypeImpl<org.jboss.jandex.UnresolvedTypeVariable> implements TypeVariable {
    UnresolvedTypeVariableImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.UnresolvedTypeVariable jandexType) {
        super(jandexIndex, annotationOverlays, jandexType);
    }

    @Override
    public String name() {
        return jandexType.identifier();
    }

    @Override
    public List<Type> bounds() {
        return List.of(fromJandexType(jandexIndex, annotationOverlays, org.jboss.jandex.ClassType.OBJECT_TYPE));
    }
}
