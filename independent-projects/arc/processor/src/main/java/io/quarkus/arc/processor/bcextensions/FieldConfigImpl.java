package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.lang.model.declarations.FieldInfo;

class FieldConfigImpl extends DeclarationConfigImpl<org.jboss.jandex.FieldInfo, FieldConfigImpl> implements FieldConfig {
    FieldConfigImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.FieldInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public FieldInfo info() {
        return new FieldInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration);
    }
}
