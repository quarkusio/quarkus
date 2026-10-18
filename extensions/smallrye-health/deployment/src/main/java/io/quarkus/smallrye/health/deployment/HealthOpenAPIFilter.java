package io.quarkus.smallrye.health.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.tags.Tag;

/**
 * Create OpenAPI entries (if configured)
 */
public class HealthOpenAPIFilter implements OASFilter {

    private static final List<String> MICROPROFILE_HEALTH_TAG = Collections.singletonList("MicroProfile Health");
    private static final String HEALTH_RESPONSE_SCHEMA_NAME = "HealthResponse";
    private static final String HEALTH_CHECK_SCHEMA_NAME = "HealthCheck";

    private static final Schema healthResponseSchemaDefinition = OASFactory.createSchema()
            .type(Collections.singletonList(Schema.SchemaType.OBJECT))
            .properties(new TreeMap<>(Map.ofEntries(
                    Map.entry("status",
                            OASFactory.createSchema()
                                    .type(Collections.singletonList(Schema.SchemaType.STRING))
                                    .enumeration(List.of("UP", "DOWN"))),

                    Map.entry("checks",
                            OASFactory.createSchema()
                                    .type(Collections.singletonList(Schema.SchemaType.ARRAY))
                                    .items(OASFactory.createSchema()
                                            .ref("#/components/schemas/" + HEALTH_CHECK_SCHEMA_NAME))))));

    private static final Schema healthCheckSchemaDefinition = OASFactory.createSchema()
            .type(Collections.singletonList(Schema.SchemaType.OBJECT))
            .properties(new TreeMap<>(Map.ofEntries(
                    Map.entry("name",
                            OASFactory.createSchema()
                                    .type(Collections.singletonList(Schema.SchemaType.STRING))),

                    Map.entry("status",
                            OASFactory.createSchema()
                                    .type(Collections.singletonList(Schema.SchemaType.STRING))
                                    .enumeration(List.of("UP", "DOWN"))),

                    Map.entry("data",
                            OASFactory.createSchema()
                                    .type(List.of(Schema.SchemaType.OBJECT, Schema.SchemaType.NULL))))));

    private final String rootPath;
    private final String livenessPath;
    private final String readinessPath;
    private final String startupPath;

    public HealthOpenAPIFilter(String rootPath, String livenessPath, String readinessPath, String startupPath) {
        this.rootPath = rootPath;
        this.livenessPath = livenessPath;
        this.readinessPath = readinessPath;
        this.startupPath = startupPath;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }

        openAPI.getComponents().addSchema(HEALTH_RESPONSE_SCHEMA_NAME, healthResponseSchemaDefinition);
        openAPI.getComponents().addSchema(HEALTH_CHECK_SCHEMA_NAME, healthCheckSchemaDefinition);

        if (openAPI.getPaths() == null) {
            openAPI.setPaths(OASFactory.createPaths());
        }

        Tag tag = OASFactory.createTag();
        tag.setName(MICROPROFILE_HEALTH_TAG.get(0));
        tag.setDescription("Check the health of the application");
        openAPI.addTag(tag);

        final Paths paths = openAPI.getPaths();

        // Health
        paths.addPathItem(
                rootPath,
                createHealthEndpoint(
                        "MicroProfile Health Endpoint",
                        "MicroProfile Health provides a way for your application to distribute " +
                                "information about its healthiness state to state whether or not it is able to " +
                                "function properly",
                        "Check the health of the application",
                        "microprofile_health_root",
                        "An aggregated view of the Liveness, Readiness and Startup of this application"));

        // Liveness
        paths.addPathItem(
                livenessPath,
                createHealthEndpoint(
                        "MicroProfile Health - Liveness Endpoint",
                        "Liveness checks are utilized to tell whether the application should be " +
                                "restarted",
                        "Check the liveness of the application",
                        "microprofile_health_liveness",
                        "The Liveness check of this application"));

        // Readiness
        paths.addPathItem(
                readinessPath,
                createHealthEndpoint(
                        "MicroProfile Health - Readiness Endpoint",
                        "Readiness checks are used to tell whether the application is able to " +
                                "process requests",
                        "Check the readiness of the application",
                        "microprofile_health_readiness",
                        "The Readiness check of this application"));

        // Startup
        paths.addPathItem(
                startupPath,
                createHealthEndpoint(
                        "MicroProfile Health - Startup Endpoint",
                        "Startup checks are an used to tell when the application has started",
                        "Check the startup of the application",
                        "microprofile_health_startup",
                        "The Startup check of this application"));
    }

    /**
     * Creates a {@link PathItem} containing the endpoint definition and GET {@link Operation} for health endpoints.
     *
     * @param endpointDescription The description for the endpoint definition
     * @param endpointSummary The summary for the endpoint definition
     * @param operationDescription The description for the operation definition
     * @param operationId The operation-id for the operation definition
     * @param operationSummary The summary for the operation definition
     */
    private PathItem createHealthEndpoint(
            String endpointDescription,
            String endpointSummary,
            String operationDescription,
            String operationId,
            String operationSummary) {
        final Content content = OASFactory.createContent()
                .addMediaType(
                        "application/json",
                        OASFactory.createMediaType()
                                .schema(OASFactory.createSchema().ref("#/components/schemas/" + HEALTH_RESPONSE_SCHEMA_NAME)));

        final APIResponses responses = OASFactory.createAPIResponses()
                .addAPIResponse(
                        "200",
                        OASFactory.createAPIResponse().description("OK").content(content))
                .addAPIResponse(
                        "503",
                        OASFactory.createAPIResponse().description("Service Unavailable").content(content))
                .addAPIResponse(
                        "500",
                        OASFactory.createAPIResponse().description("Internal Server Error").content(content));

        final Operation getOperation = OASFactory.createOperation()
                .operationId(operationId)
                .description(operationDescription)
                .tags(MICROPROFILE_HEALTH_TAG)
                .summary(operationSummary)
                .responses(responses);

        return OASFactory.createPathItem()
                .description(endpointDescription)
                .summary(endpointSummary)
                .GET(getOperation);
    }
}
