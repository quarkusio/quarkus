package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.lang.model.declarations.PackageInfo;

class PackageInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.ClassInfo> implements PackageInfo {
    PackageInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.ClassInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public String name() {
        return jandexDeclaration.name().packagePrefix();
    }
}
