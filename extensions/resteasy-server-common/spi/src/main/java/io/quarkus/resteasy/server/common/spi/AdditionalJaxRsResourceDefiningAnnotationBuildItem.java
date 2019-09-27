package io.quarkus.resteasy.server.common.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalJaxRsResourceDefiningAnnotationBuildItem extends MultiBuildItem {

    private final DotName annotationClass;

    public AdditionalJaxRsResourceDefiningAnnotationBuildItem(DotName annotationClass) {
        this.annotationClass = annotationClass;
    }

    public DotName getAnnotationClass() {
        return annotationClass;
    }
}
