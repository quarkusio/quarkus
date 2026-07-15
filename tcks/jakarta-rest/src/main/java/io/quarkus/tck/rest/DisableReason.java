package io.quarkus.tck.rest;

public enum DisableReason {

    UNSUPPORTED_XML("XML/JAXB is not supported"),
    UNSUPPORTED_DATASOURCE("DataSource is not supported"),
    UNSUPPORTED_SOURCE("javax.xml.transform.Source is not supported"),
    UNSUPPORTED_STREAMING_OUTPUT("StreamingOutput is not supported on the client"),
    UNSUPPORTED_APPLICATION_SINGLETONS("Application singletons are not supported"),
    UNSUPPORTED_CLIENT_SERVER_INJECTION_SEPARATION("Injection separation bug between client and server"),
    UNSUPPORTED("Not supported in Quarkus REST"),
    UNSUPPORTED_INJECTION_OF_PATH_PARAM_BEFORE_RESOURCE_LOCATOR_IS_KNOWN(
            "Requires field injection of a path param before the locator method is known"),
    UNSUPPORTED_PATH_SEGMENT_PARAMETER_WITH_MATRIX_PARAMS(
            "Requires PathParam to keep track of which path segment they belong to"),
    LOCATOR_ISSUES("Resource locator related issues"),
    ENCODED("@Encoded in paths is not supported"),
    NUTS("Test does not make sense"),
    TEST_DOESNT_MAKE_SENSE("Test is not in accordance with the spec"),
    UNDERSPECIFIED("Spec is not clear about expected behavior"),
    NOT_IMPLEMENTED_YET("Not yet implemented in Quarkus REST"),
    FILE_HANDLING("File handling should be done by Vert.x"),
    SIGNATURE_TEST("Signature test library not available"),
    THREADING_MODEL("Threading model incompatibility"),
    EMPTY_PARAM_IS_NULL("Quarkus REST deliberately treats empty parameter values as null"),
    CLIENT_EXCEPTION_WRAPPING(
            "Client exceptions wrapped in ClientWebApplicationException for security reasons");

    private final String description;

    DisableReason(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
