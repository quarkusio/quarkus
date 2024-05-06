package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.ClassType;

import org.jboss.jandex.DotName;

class ClassTypeImpl extends TypeImpl<org.jboss.jandex.ClassType> implements ClassType {
    ClassTypeImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.ClassType jandexType) {
        super(jandexIndex, annotationOverlay, jandexType);
    }

    @Override
    public ClassInfo declaration() {
        DotName name = jandexType.name();
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexIndex.getClassByName(name));
    }
}
