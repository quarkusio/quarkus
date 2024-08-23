package io.quarkus.arc.processor.bcextensions;

import org.jboss.jandex.DotName;

enum ExtensionPhase {
    DISCOVERY(DotNames.DISCOVERY),
    ENHANCEMENT(DotNames.ENHANCEMENT),
    REGISTRATION(DotNames.REGISTRATION),
    SYNTHESIS(DotNames.SYNTHESIS),
    VALIDATION(DotNames.VALIDATION),
    ;

    final DotName annotationName;

    ExtensionPhase(DotName annotationName) {
        this.annotationName = annotationName;
    }

    @Override
    public String toString() {
        return "@" + annotationName.withoutPackagePrefix();
    }
}
