package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.inject.build.compatible.spi.ParameterConfig;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

class MethodConfigImpl extends DeclarationConfigImpl<org.jboss.jandex.MethodInfo, MethodConfigImpl> implements MethodConfig {
    MethodConfigImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.MethodInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public MethodInfo info() {
        return new MethodInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public List<ParameterConfig> parameters() {
        List<ParameterConfig> result = new ArrayList<>(jandexDeclaration.parametersCount());
        for (org.jboss.jandex.MethodParameterInfo jandexParameter : jandexDeclaration.parameters()) {
            result.add(new ParameterConfigImpl(jandexIndex, annotationOverlay, jandexParameter));
        }
        return Collections.unmodifiableList(result);
    }
}
