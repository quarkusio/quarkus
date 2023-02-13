package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.lang.model.declarations.FieldInfo;

class FieldConfigImpl extends DeclarationConfigImpl<org.jboss.jandex.FieldInfo, FieldConfigImpl> implements FieldConfig {
    FieldConfigImpl(org.jboss.jandex.IndexView jandexIndex, AllAnnotationTransformations allTransformations,
            org.jboss.jandex.FieldInfo jandexDeclaration) {
        super(jandexIndex, allTransformations, allTransformations.fields, jandexDeclaration);
    }

    @Override
    public FieldInfo info() {
        return new FieldInfoImpl(jandexIndex, allTransformations.annotationOverlays, jandexDeclaration);
    }
}
