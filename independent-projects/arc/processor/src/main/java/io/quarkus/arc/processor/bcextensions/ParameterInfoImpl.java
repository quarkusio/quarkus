package io.quarkus.arc.processor.bcextensions;

import java.util.Objects;

import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;
import jakarta.enterprise.lang.model.types.Type;

class ParameterInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.MethodParameterInfo> implements ParameterInfo {
    // only for equals/hashCode
    private final MethodInfoImpl method;
    private final short position;

    ParameterInfoImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.MethodParameterInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlays, jandexDeclaration);
        this.method = new MethodInfoImpl(jandexIndex, annotationOverlays, jandexDeclaration.method());
        this.position = jandexDeclaration.position();
    }

    @Override
    public String name() {
        String name = jandexDeclaration.name();
        return name != null ? name : "arg" + jandexDeclaration.position();
    }

    @Override
    public Type type() {
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlays, jandexDeclaration.type());
    }

    @Override
    public MethodInfo declaringMethod() {
        return new MethodInfoImpl(jandexIndex, annotationOverlays, jandexDeclaration.method());
    }

    @Override
    public String toString() {
        return "parameter " + name() + " of method " + jandexDeclaration.method();
    }

    @Override
    AnnotationsOverlay<org.jboss.jandex.MethodParameterInfo> annotationsOverlay() {
        return annotationOverlays.parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ParameterInfoImpl that = (ParameterInfoImpl) o;
        return position == that.position
                && Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, position);
    }
}
