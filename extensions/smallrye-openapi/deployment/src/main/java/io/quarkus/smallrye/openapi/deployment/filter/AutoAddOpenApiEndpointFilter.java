package io.quarkus.smallrye.openapi.deployment.filter;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

public class AutoAddOpenApiEndpointFilter implements OASFilter {
    private static final String OPENAPI_TAG = "openapi";
    private static final String ENDPOINT_DESCRIPTION = "Open API specification";

    private final String path;

    public AutoAddOpenApiEndpointFilter(String path) {
        this.path = path;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null) {
            paths = OASFactory.createPaths();
            openAPI.setPaths(paths);
        }
        openAPI.addTag(OASFactory.createTag().name(OPENAPI_TAG));
        paths.addPathItem(
                path,
                OASFactory.createPathItem()
                        .GET(
                                OASFactory.createOperation()
                                        .description(ENDPOINT_DESCRIPTION)
                                        .addTag(OPENAPI_TAG)
                                        .operationId("getOpenapiJSON")
                                        .responses(createOpenApiResponse())));
    }

    private static APIResponses createOpenApiResponse() {
        Content jsonContent = OASFactory.createContent()
                .addMediaType("application/json", OASFactory.createMediaType());
        return OASFactory.createAPIResponses()
                .addAPIResponse(
                        "200",
                        OASFactory.createAPIResponse()
                                .description(ENDPOINT_DESCRIPTION)
                                .content(jsonContent));
    }
}
