package io.quarkus.arc.processor.bcextensions;

import java.lang.reflect.Modifier;
import java.util.Objects;

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.jandex.DotName;

class FieldInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.FieldInfo> implements FieldInfo {
    // only for equals/hashCode
    private final DotName className;
    private final String name;

    FieldInfoImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.FieldInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlays, jandexDeclaration);
        this.className = jandexDeclaration.declaringClass().name();
        this.name = jandexDeclaration.name();
    }

    @Override
    public String name() {
        return jandexDeclaration.name();
    }

    @Override
    public Type type() {
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlays, jandexDeclaration.type());
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
        return new ClassInfoImpl(jandexIndex, annotationOverlays, jandexDeclaration.declaringClass());
    }

    @Override
    AnnotationsOverlay<org.jboss.jandex.FieldInfo> annotationsOverlay() {
        return annotationOverlays.fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FieldInfoImpl fieldInfo = (FieldInfoImpl) o;
        return Objects.equals(className, fieldInfo.className)
                && Objects.equals(name, fieldInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, name);
    }
}
