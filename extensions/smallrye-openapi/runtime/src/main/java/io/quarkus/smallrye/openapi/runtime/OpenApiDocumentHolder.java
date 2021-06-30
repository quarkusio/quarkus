package io.quarkus.smallrye.openapi.runtime;

import io.smallrye.openapi.runtime.io.Format;

/**
 * Holds instances of the OpenAPI Document
 */
public interface OpenApiDocumentHolder {

    public byte[] getJsonDocument();

    public byte[] getYamlDocument();

    default byte[] getDocument(Format format) {
        if (format.equals(Format.JSON)) {
            return getJsonDocument();
        }
        return getYamlDocument();
    }
}
