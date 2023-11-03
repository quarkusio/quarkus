package io.quarkus.arc.processor.bcextensions;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.lang.model.types.ClassType;
import jakarta.enterprise.lang.model.types.ParameterizedType;
import jakarta.enterprise.lang.model.types.Type;

class ParameterizedTypeImpl extends TypeImpl<org.jboss.jandex.ParameterizedType> implements ParameterizedType {
    ParameterizedTypeImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.ParameterizedType jandexType) {
        super(jandexIndex, annotationOverlays, jandexType);
    }

    @Override
    public ClassType genericClass() {
        org.jboss.jandex.Type jandexClassType = org.jboss.jandex.Type.create(jandexType.name(),
                org.jboss.jandex.Type.Kind.CLASS);
        return new ClassTypeImpl(jandexIndex, annotationOverlays, (org.jboss.jandex.ClassType) jandexClassType);
    }

    @Override
    public List<Type> typeArguments() {
        return jandexType.arguments()
                .stream()
                .map(it -> fromJandexType(jandexIndex, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }
}
