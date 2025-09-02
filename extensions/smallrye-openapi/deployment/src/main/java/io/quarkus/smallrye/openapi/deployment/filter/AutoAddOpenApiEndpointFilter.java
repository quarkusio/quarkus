package io.quarkus.smallrye.openapi.deployment.filter;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Content;

public class AutoAddOpenApiEndpointFilter implements OASFilter {
    private static final String OPENAPI_TAG = "openapi";
    private static final String ENDPOINT_DESCRIPTION = "OpenAPI specification";

    private final String path;

    private enum MediaTypeForEndpoint {
        JSON,
        YAML,
        BOTH,
    }

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
        createPathItem(paths, "", MediaTypeForEndpoint.BOTH);
        createPathItem(paths, "Json", MediaTypeForEndpoint.JSON);
        createPathItem(paths, "Yaml", MediaTypeForEndpoint.YAML);
        createPathItem(paths, "Yml", MediaTypeForEndpoint.YAML);
    }

    private void createPathItem(Paths paths, String suffix, MediaTypeForEndpoint mediaTypeForEndpoints) {
        Content openApiContent = OASFactory.createContent();
        if (mediaTypeForEndpoints == MediaTypeForEndpoint.JSON || mediaTypeForEndpoints == MediaTypeForEndpoint.BOTH) {
            openApiContent.addMediaType("application/json", OASFactory.createMediaType());
        }
        if (mediaTypeForEndpoints == MediaTypeForEndpoint.YAML || mediaTypeForEndpoints == MediaTypeForEndpoint.BOTH) {
            openApiContent.addMediaType("application/yaml", OASFactory.createMediaType());
        }
        var openApiResponse = OASFactory.createAPIResponses()
                .addAPIResponse(
                        "200",
                        OASFactory.createAPIResponse()
                                .description(ENDPOINT_DESCRIPTION)
                                .content(openApiContent));
        var pathItemPath = this.path;
        // Strip off dot
        if (!suffix.isEmpty()) {
            pathItemPath += "." + suffix.toLowerCase();
        }
        var pathItem = OASFactory.createPathItem()
                .GET(
                        OASFactory.createOperation()
                                .description(ENDPOINT_DESCRIPTION)
                                .addTag(OPENAPI_TAG)
                                .operationId("getOpenAPISpecification" + suffix)
                                .responses(openApiResponse));
        paths.addPathItem(pathItemPath, pathItem);
    }
}
