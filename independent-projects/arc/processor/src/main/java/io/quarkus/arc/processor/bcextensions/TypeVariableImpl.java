package io.quarkus.arc.processor.bcextensions;

import java.util.List;

import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.TypeVariable;

class TypeVariableImpl extends TypeImpl<org.jboss.jandex.TypeVariable> implements TypeVariable {
    TypeVariableImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.TypeVariable jandexType) {
        super(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public String name() {
        return jandexType.identifier();
    }

    @Override
    public List<Type> bounds() {
        return jandexType.bounds()
                .stream()
                .map(it -> fromJandexType(jandexIndex, annotationOverlay, it))
                .toList();
    }
}
