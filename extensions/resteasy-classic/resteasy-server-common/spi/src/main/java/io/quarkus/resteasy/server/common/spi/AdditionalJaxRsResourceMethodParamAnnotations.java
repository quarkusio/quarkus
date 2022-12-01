package io.quarkus.resteasy.server.common.spi;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalJaxRsResourceMethodParamAnnotations extends MultiBuildItem {

    private final List<DotName> annotationClasses;

    public AdditionalJaxRsResourceMethodParamAnnotations(List<DotName> annotationClasses) {
        this.annotationClasses = annotationClasses;
    }

    public List<DotName> getAnnotationClasses() {
        return annotationClasses;
    }
}
