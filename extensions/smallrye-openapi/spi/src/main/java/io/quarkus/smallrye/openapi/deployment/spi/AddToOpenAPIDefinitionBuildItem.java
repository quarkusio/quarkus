package io.quarkus.smallrye.openapi.deployment.spi;

import org.eclipse.microprofile.openapi.OASFilter;

import io.quarkus.builder.item.MultiBuildItem;

public final class AddToOpenAPIDefinitionBuildItem extends MultiBuildItem {

    private final OASFilter filter;
    private final String documentName;

    /**
     * Applies to the default OpenAPI document
     *
     * @param filter the filter to be applied when building the OpenAPI document
     */
    @Deprecated
    public AddToOpenAPIDefinitionBuildItem(OASFilter filter) {
        this(filter, OpenAPISPIConstants.DEFAULT_DOCUMENT_NAME);
    }

    /**
     * @param filter the filter to be applied when building the OpenAPI document
     * @param documentName the name of the document this filter applies to, null for all documents, or
     *        {@link OpenAPISPIConstants#DEFAULT_DOCUMENT_NAME} for the default document
     */
    public AddToOpenAPIDefinitionBuildItem(OASFilter filter, String documentName) {
        this.filter = filter;
        this.documentName = documentName;
    }

    public OASFilter getOASFilter() {
        return this.filter;
    }

    /**
     * The name of the OpenAPI document this BuildItem should be applied to, null if it should be applied to all OpenAPI
     * documents, or {@link OpenAPISPIConstants#DEFAULT_DOCUMENT_NAME} for the default document.
     */
    public String getDocumentName() {
        return documentName;
    }
}
