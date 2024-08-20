package io.quarkus.smallrye.health.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;

/**
 * Create OpenAPI entries (if configured)
 */
public class HealthOpenAPIFilter implements OASFilter {

    private static final List<String> MICROPROFILE_HEALTH_TAG = Collections.singletonList("MicroProfile Health");
    private static final String HEALTH_RESPONSE_SCHEMA_NAME = "HealthResponse";
    private static final String HEALTH_CHECK_SCHEMA_NAME = "HealthCheck";

    private static final Schema healthResponseSchemaDefinition = new SchemaImpl(HEALTH_RESPONSE_SCHEMA_NAME)
            .type(Schema.SchemaType.OBJECT)
            .properties(Map.ofEntries(

                    Map.entry("status",
                            new SchemaImpl()
                                    .type(Schema.SchemaType.STRING)
                                    .enumeration(List.of("UP", "DOWN"))),

                    Map.entry("checks",
                            new SchemaImpl()
                                    .type(Schema.SchemaType.ARRAY)
                                    .items(new SchemaImpl().ref("#/components/schemas/" + HEALTH_CHECK_SCHEMA_NAME)))));

    private static final Schema healthCheckSchemaDefinition = new SchemaImpl(HEALTH_CHECK_SCHEMA_NAME)
            .type(Schema.SchemaType.OBJECT)
            .properties(Map.ofEntries(

                    Map.entry("name",
                            new SchemaImpl()
                                    .type(Schema.SchemaType.STRING)),

                    Map.entry("status",
                            new SchemaImpl()
                                    .type(Schema.SchemaType.STRING)
                                    .enumeration(List.of("UP", "DOWN"))),

                    Map.entry("data",
                            new SchemaImpl()
                                    .type(Schema.SchemaType.OBJECT)
                                    .nullable(Boolean.TRUE))));

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
            openAPI.setComponents(new ComponentsImpl());
        }
        openAPI.getComponents().addSchema(HEALTH_RESPONSE_SCHEMA_NAME, healthResponseSchemaDefinition);
        openAPI.getComponents().addSchema(HEALTH_CHECK_SCHEMA_NAME, healthCheckSchemaDefinition);

        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new PathsImpl());
        }

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
        final Content content = new ContentImpl()
                .addMediaType(
                        "application/json",
                        new MediaTypeImpl()
                                .schema(new SchemaImpl().ref("#/components/schemas/" + HEALTH_RESPONSE_SCHEMA_NAME)));

        final APIResponses responses = new APIResponsesImpl()
                .addAPIResponse(
                        "200",
                        new APIResponseImpl().description("OK").content(content))
                .addAPIResponse(
                        "503",
                        new APIResponseImpl().description("Service Unavailable").content(content))
                .addAPIResponse(
                        "500",
                        new APIResponseImpl().description("Internal Server Error").content(content));

        final Operation getOperation = new OperationImpl()
                .operationId(operationId)
                .description(operationDescription)
                .tags(MICROPROFILE_HEALTH_TAG)
                .summary(operationSummary)
                .responses(responses);

        return new PathItemImpl()
                .description(endpointDescription)
                .summary(endpointSummary)
                .GET(getOperation);
    }
}
