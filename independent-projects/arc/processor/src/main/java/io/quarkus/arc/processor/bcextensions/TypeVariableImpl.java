package io.quarkus.arc.processor.bcextensions;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.TypeVariable;

class TypeVariableImpl extends TypeImpl<org.jboss.jandex.TypeVariable> implements TypeVariable {
    TypeVariableImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.TypeVariable jandexType) {
        super(jandexIndex, annotationOverlays, jandexType);
    }

    @Override
    public String name() {
        return jandexType.identifier();
    }

    @Override
    public List<Type> bounds() {
        return jandexType.bounds()
                .stream()
                .map(it -> fromJandexType(jandexIndex, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }
}
