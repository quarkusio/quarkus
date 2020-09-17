package io.quarkus.smallrye.openapi.runtime;

public class OpenApiConstants {

    /*
     * <em>Ugly Hack</em>
     * In dev mode, we receive a classloader to load the up to date OpenAPI document.
     * This hack is required because using the TCCL would get an outdated version - the initial one.
     * This is because the worker thread on which the handler is called captures the TCCL at creation time
     * and does not allow updating it.
     *
     * This classloader must ONLY be used to load the OpenAPI document.
     *
     * In non dev mode, the TCCL is used.
     *
     * TODO: remove this once the vert.x class loader issues are resolved.
     */
    public static volatile ClassLoader classLoader;

    public static final String GENERATED_DOC_BASE = "quarkus-generated-openapi-doc.";
    public static final String BASE_NAME = "META-INF/" + GENERATED_DOC_BASE;

    private OpenApiConstants() {
    }

}
