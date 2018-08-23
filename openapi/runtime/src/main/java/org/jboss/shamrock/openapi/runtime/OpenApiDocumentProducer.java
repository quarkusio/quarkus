package org.jboss.shamrock.openapi.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.smallrye.openapi.api.OpenApiDocument;

/**
 * @author Ken Finnigan
 */
@ApplicationScoped
public class OpenApiDocumentProducer {
    private OpenApiDocument document;

    @Produces
    @Dependent
    OpenApiDocument openApiDocument() {
        return this.document;
    }

    void setDocument(OpenApiDocument document) {
        this.document = document;
    }
}
