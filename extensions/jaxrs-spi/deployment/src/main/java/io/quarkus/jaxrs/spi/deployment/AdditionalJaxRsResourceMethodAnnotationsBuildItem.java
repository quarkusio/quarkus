package io.quarkus.jaxrs.spi.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalJaxRsResourceMethodAnnotationsBuildItem extends MultiBuildItem {

    private final List<DotName> annotationClasses;

    public AdditionalJaxRsResourceMethodAnnotationsBuildItem(List<DotName> annotationClasses) {
        this.annotationClasses = annotationClasses;
    }

    public List<DotName> getAnnotationClasses() {
        return annotationClasses;
    }
}
