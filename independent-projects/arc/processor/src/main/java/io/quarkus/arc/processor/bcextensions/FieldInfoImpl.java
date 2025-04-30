package io.quarkus.arc.processor.bcextensions;

import java.lang.reflect.Modifier;

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.types.Type;

class FieldInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.FieldInfo> implements FieldInfo {
    FieldInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.FieldInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public String name() {
        return jandexDeclaration.name();
    }

    @Override
    public Type type() {
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, jandexDeclaration.type());
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(jandexDeclaration.flags());
    }

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(jandexDeclaration.flags());
    }

    @Override
    public int modifiers() {
        return jandexDeclaration.flags();
    }

    @Override
    public ClassInfo declaringClass() {
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration.declaringClass());
    }
}
