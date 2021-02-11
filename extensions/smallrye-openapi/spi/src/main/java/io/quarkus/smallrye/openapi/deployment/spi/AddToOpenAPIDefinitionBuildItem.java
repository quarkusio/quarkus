package io.quarkus.smallrye.openapi.deployment.spi;

import org.eclipse.microprofile.openapi.OASFilter;

import io.quarkus.builder.item.MultiBuildItem;

public final class AddToOpenAPIDefinitionBuildItem extends MultiBuildItem {

    private final OASFilter filter;

    /**
     * @param filter the filter to be applied when building the OpenAPI document
     */
    public AddToOpenAPIDefinitionBuildItem(OASFilter filter) {
        this.filter = filter;
    }

    public OASFilter getOASFilter() {
        return this.filter;
    }
}
