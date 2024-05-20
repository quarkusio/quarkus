package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.RecordComponentInfo;
import jakarta.enterprise.lang.model.types.Type;

class RecordComponentInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.RecordComponentInfo> implements RecordComponentInfo {
    public RecordComponentInfoImpl(org.jboss.jandex.IndexView jandexIndex,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.RecordComponentInfo recordComponentInfo) {
        super(jandexIndex, annotationOverlay, recordComponentInfo);
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
    public FieldInfo field() {
        return new FieldInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration.field());
    }

    @Override
    public MethodInfo accessor() {
        return new MethodInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration.accessor());
    }

    @Override
    public ClassInfo declaringRecord() {
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration.declaringClass());
    }
}
