package io.quarkus.smallrye.openapi.runtime;

/**
 * Holds instances of the OpenAPI Document
 */
public interface OpenApiDocumentHolder {

    public byte[] getJsonDocument();

    public byte[] getYamlDocument();

}
