package io.quarkus.arc.processor.bcextensions;

import java.util.List;

import jakarta.enterprise.lang.model.types.ClassType;
import jakarta.enterprise.lang.model.types.ParameterizedType;
import jakarta.enterprise.lang.model.types.Type;

class ParameterizedTypeImpl extends TypeImpl<org.jboss.jandex.ParameterizedType> implements ParameterizedType {
    ParameterizedTypeImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.ParameterizedType jandexType) {
        super(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public ClassType genericClass() {
        org.jboss.jandex.Type jandexClassType = org.jboss.jandex.Type.create(jandexType.name(),
                org.jboss.jandex.Type.Kind.CLASS);
        return new ClassTypeImpl(jandexIndex, annotationOverlay, (org.jboss.jandex.ClassType) jandexClassType);
    }

    @Override
    public List<Type> typeArguments() {
        return jandexType.arguments()
                .stream()
                .map(it -> fromJandexType(jandexIndex, annotationOverlay, it))
                .toList();
    }
}
