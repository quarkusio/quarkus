package io.quarkus.smallrye.openapi.runtime.produi;

import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService;
import io.smallrye.openapi.runtime.io.Format;

/**
 * Read-only Prod UI view of the running application's OpenAPI schema. It exposes
 * the generated default OpenAPI document (JSON) derived from the always-present
 * {@link OpenApiDocumentService} bean. This is the same document already served
 * by the public {@code /q/openapi} endpoint, so no additional information is
 * exposed; the component derives the operation list from it client-side.
 */
@ApplicationScoped
public class SmallRyeOpenApiProdUIService {

    @Inject
    OpenApiDocumentService openApiDocumentService;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get the running application's OpenAPI schema document (JSON) for the default document")
    public String getOpenAPISchema() {
        return new String(openApiDocumentService.getDocument(OpenApiConstants.DEFAULT_DOCUMENT_NAME, Format.JSON),
                StandardCharsets.UTF_8);
    }
}
