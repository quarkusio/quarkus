package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.ParameterConfig;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;

class ParameterConfigImpl extends DeclarationConfigImpl<org.jboss.jandex.MethodParameterInfo, ParameterConfigImpl>
        implements ParameterConfig {
    ParameterConfigImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.MethodParameterInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public ParameterInfo info() {
        return new ParameterInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration);
    }
}
