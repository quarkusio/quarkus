package io.quarkus.smallrye.health.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
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

    private final String rootPath;
    private final String livenessPath;
    private final String readinessPath;

    public HealthOpenAPIFilter(String rootPath, String livenessPath, String readinessPath) {
        this.rootPath = rootPath;
        this.livenessPath = livenessPath;
        this.readinessPath = readinessPath;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        openAPI.getComponents().addSchema("HealthCheckResponse", createHealthCheckResponse());
        openAPI.getComponents().addSchema("State", createState());

        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new PathsImpl());
        }
        Paths paths = openAPI.getPaths();

        // Health
        paths.addPathItem(rootPath, createHealthPathItem());

        // Liveness
        paths.addPathItem(livenessPath, createLivenessPathItem());

        // Readiness
        paths.addPathItem(readinessPath, createReadinessPathItem());
    }

    private PathItem createHealthPathItem() {
        PathItem pathItem = new PathItemImpl();
        pathItem.setDescription("MicroProfile Health Endpoint");
        pathItem.setSummary(
                "MicroProfile Health provides a way for your application to distribute information about its healthiness state to state whether or not it is able to function properly");
        pathItem.setGET(createHealthOperation());
        return pathItem;
    }

    private PathItem createLivenessPathItem() {
        PathItem pathItem = new PathItemImpl();
        pathItem.setDescription("MicroProfile Health - Liveness Endpoint");
        pathItem.setSummary(
                "Liveness checks are utilized to tell whether the application should be restarted");
        pathItem.setGET(createLivenessOperation());
        return pathItem;
    }

    private PathItem createReadinessPathItem() {
        PathItem pathItem = new PathItemImpl();
        pathItem.setDescription("MicroProfile Health - Readiness Endpoint");
        pathItem.setSummary(
                "Readiness checks are used to tell whether the application is able to process requests");
        pathItem.setGET(createReadinessOperation());
        return pathItem;
    }

    private Operation createHealthOperation() {
        Operation operation = new OperationImpl();
        operation.setDescription("Check the health of the application");
        operation.setOperationId("microprofile_health_root");
        operation.setTags(MICROPROFILE_HEALTH_TAG);
        operation.setSummary("An aggregated view of the Liveness and Readiness of this application");
        operation.setResponses(createAPIResponses());
        return operation;
    }

    private Operation createLivenessOperation() {
        Operation operation = new OperationImpl();
        operation.setDescription("Check the liveness of the application");
        operation.setOperationId("microprofile_health_liveness");
        operation.setTags(MICROPROFILE_HEALTH_TAG);
        operation.setSummary("The Liveness check of this application");
        operation.setResponses(createAPIResponses());
        return operation;
    }

    private Operation createReadinessOperation() {
        Operation operation = new OperationImpl();
        operation.setDescription("Check the readiness of the application");
        operation.setOperationId("microprofile_health_readiness");
        operation.setTags(MICROPROFILE_HEALTH_TAG);
        operation.setSummary("The Readiness check of this application");
        operation.setResponses(createAPIResponses());
        return operation;
    }

    private APIResponses createAPIResponses() {
        APIResponses responses = new APIResponsesImpl();
        responses.addAPIResponse("200", createAPIResponse());
        responses.addAPIResponse("503", createAPIResponse());
        responses.addAPIResponse("500", createAPIResponse());
        return responses;
    }

    private APIResponse createAPIResponse() {
        APIResponse response = new APIResponseImpl();
        response.setContent(createContent());
        return response;
    }

    private Content createContent() {
        Content content = new ContentImpl();
        content.addMediaType("application/json", createMediaType());
        return content;
    }

    private MediaType createMediaType() {
        MediaType mediaType = new MediaTypeImpl();
        mediaType.setSchema(new SchemaImpl().ref("#/components/schemas/HealthCheckResponse"));
        return mediaType;
    }

    /**
     * HealthCheckResponse:
     * type: object
     * properties:
     * data:
     * type: object
     * nullable: true
     * name:
     * type: string
     * state:
     * $ref: '#/components/schemas/State'
     * 
     * @return Schema representing HealthCheckResponse
     */
    private Schema createHealthCheckResponse() {
        Schema schema = new SchemaImpl("HealthCheckResponse");
        schema.setType(Schema.SchemaType.OBJECT);
        schema.setProperties(createProperties());
        return schema;
    }

    private Map<String, Schema> createProperties() {
        Map<String, Schema> map = new HashMap<>();
        map.put("data", createData());
        map.put("name", createName());
        map.put("state", new SchemaImpl().ref("#/components/schemas/State"));
        return map;
    }

    private Schema createData() {
        Schema schema = new SchemaImpl("data");
        schema.setType(Schema.SchemaType.OBJECT);
        schema.setNullable(Boolean.TRUE);
        return schema;
    }

    private Schema createName() {
        Schema schema = new SchemaImpl("name");
        schema.setType(Schema.SchemaType.STRING);
        return schema;
    }

    /**
     * State:
     * enum:
     * - DOWN
     * - UP
     * type: string
     * 
     * @return Schema representing State
     */
    private Schema createState() {
        Schema schema = new SchemaImpl("State");
        schema.setEnumeration(createStateEnumValues());
        schema.setType(Schema.SchemaType.STRING);
        return schema;
    }

    private List<Object> createStateEnumValues() {
        List<Object> values = new ArrayList<>();
        values.add("DOWN");
        values.add("UP");
        return values;
    }
}
