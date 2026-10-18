package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

class ClassConfigImpl extends DeclarationConfigImpl<org.jboss.jandex.ClassInfo, ClassConfigImpl> implements ClassConfig {
    ClassConfigImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.ClassInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public ClassInfo info() {
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public Collection<MethodConfig> constructors() {
        List<MethodConfig> result = new ArrayList<>();
        for (MethodInfo constructor : info().constructors()) {
            result.add(new MethodConfigImpl(jandexIndex, annotationOverlay, ((MethodInfoImpl) constructor).jandexDeclaration));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<MethodConfig> methods() {
        List<MethodConfig> result = new ArrayList<>();
        for (MethodInfo method : info().methods()) {
            result.add(new MethodConfigImpl(jandexIndex, annotationOverlay, ((MethodInfoImpl) method).jandexDeclaration));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<FieldConfig> fields() {
        List<FieldConfig> result = new ArrayList<>();
        for (FieldInfo field : info().fields()) {
            result.add(new FieldConfigImpl(jandexIndex, annotationOverlay, ((FieldInfoImpl) field).jandexDeclaration));
        }
        return Collections.unmodifiableList(result);
    }
}
