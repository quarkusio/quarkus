package io.quarkus.it.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OpenApiTestCase {

    private static final String DEFAULT_MEDIA_TYPE = "application/json";
    private static final String DEFAULT_MEDIA_TYPE_PRIMITIVE = "text/plain";

    @TestHTTPResource("q/openapi")
    URL uri;

    @Test
    public void testOpenAPIJSON() throws Exception {
        URLConnection connection = uri.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = connection.getInputStream()) {
            byte[] buf = new byte[100];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
        }
        JsonReader parser = Json.createReader(new ByteArrayInputStream(out.toByteArray()));
        JsonObject obj = parser.readObject();
        Assertions.assertNotNull(obj);

        Assertions.assertEquals("3.1.0", obj.getString("openapi"));
        Assertions.assertEquals("main-integration-test API", obj.getJsonObject("info").getString("title"));
        Assertions.assertEquals("1.0", obj.getJsonObject("info").getString("version"));

        JsonObject paths = obj.getJsonObject("paths");

        JsonObject testObj = paths.getJsonObject("/test");
        Assertions.assertNotNull(testObj);
        Set<String> keys = testObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        JsonObject injectionObj = paths.getJsonObject("/test/rx");
        Assertions.assertNotNull(injectionObj);
        keys = injectionObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        // test RESTEasy extensions

        JsonObject schemasObj = obj.getJsonObject("components").getJsonObject("schemas");
        List<String> testSchemaType = schemaType("200", DEFAULT_MEDIA_TYPE_PRIMITIVE,
                testObj.getJsonObject("get").getJsonObject("responses"),
                schemasObj);
        List<String> rxSchemaType = schemaType("200", DEFAULT_MEDIA_TYPE_PRIMITIVE,
                injectionObj.getJsonObject("get").getJsonObject("responses"),
                schemasObj);
        // make sure String, CompletionStage<String> and Single<String> are detected the same
        Assertions.assertEquals(testSchemaType,
                rxSchemaType,
                "Normal and RX/Single have same schema");
        JsonObject csObj = paths.getJsonObject("/test/cs");
        Assertions.assertEquals(testSchemaType,
                schemaType("200", DEFAULT_MEDIA_TYPE_PRIMITIVE, csObj.getJsonObject("get").getJsonObject("responses"),
                        schemasObj),
                "Normal and RX/CS have same schema");

        JsonObject paramsObj = paths.getJsonObject("/test/params/{path}");
        JsonObject params2Obj = paths.getJsonObject("/test/params2/{path}");
        Assertions.assertEquals(paramsObj, params2Obj, "Normal and RESTEasy annotations have same schema");

        // test Mutiny types
        JsonObject uniObj = paths.getJsonObject("/test/uni");
        Assertions.assertNotNull(uniObj);
        keys = uniObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        List<String> uniSchemaType = schemaType("200", DEFAULT_MEDIA_TYPE_PRIMITIVE,
                uniObj.getJsonObject("get").getJsonObject("responses"),
                schemasObj);
        // make sure String, CompletionStage<String> and Uni<String> are detected the same
        Assertions.assertEquals(testSchemaType,
                uniSchemaType,
                "Normal and Mutiny Uni have same schema");

        JsonObject uniTypedObj = paths.getJsonObject("/test/uniType");
        Assertions.assertNotNull(uniTypedObj);
        keys = uniTypedObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        List<String> uniTypedSchemaType = schemaType("200", DEFAULT_MEDIA_TYPE,
                uniTypedObj.getJsonObject("get").getJsonObject("responses"),
                schemasObj);
        // make sure ComponentType and Uni<ComponentType> are detected the same
        JsonObject ctObj = paths.getJsonObject("/test/compType");
        List<String> ctSchemaType = schemaType("200", DEFAULT_MEDIA_TYPE, ctObj.getJsonObject("get").getJsonObject("responses"),
                schemasObj);
        Assertions.assertEquals(ctSchemaType,
                uniTypedSchemaType,
                "Normal and Mutiny Uni have same schema");

        JsonObject multiObj = paths.getJsonObject("/test/multi");
        Assertions.assertNotNull(multiObj);
        keys = multiObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        // make sure Multi<String> is detected as array
        JsonObject multiSchema = multiObj.getJsonObject("get").getJsonObject("responses")
                .getJsonObject("200").getJsonObject("content").getJsonObject(DEFAULT_MEDIA_TYPE).getJsonObject("schema");
        Assertions.assertEquals(List.of("array"), getTypes(multiSchema));
        Assertions.assertEquals(List.of("string"), getTypes(multiSchema.getJsonObject("items")));

        JsonObject multiTypedObj = paths.getJsonObject("/test/multiType");
        Assertions.assertNotNull(multiTypedObj);
        keys = multiTypedObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        JsonObject multiTypedSchema = multiTypedObj.getJsonObject("get").getJsonObject("responses")
                .getJsonObject("200").getJsonObject("content").getJsonObject(DEFAULT_MEDIA_TYPE).getJsonObject("schema");
        // make sure Multi<ComponentType> is detected as array
        Assertions.assertEquals(List.of("array"), getTypes(multiTypedSchema));
        List<String> mutliTypedObjectSchema = schemaTypeFromRef(multiTypedSchema.getJsonObject("items"), schemasObj);
        Assertions.assertEquals(ctSchemaType,
                mutliTypedObjectSchema,
                "Normal and Mutiny Multi have same schema");

        // Verify presence of Health API
        JsonObject healthPath = paths.getJsonObject("/q/health");
        Assertions.assertNotNull(healthPath);
        Set<String> healthKeys = healthPath.keySet();
        Assertions.assertEquals(Set.of("summary", "description", "get"), healthKeys);

        JsonObject livenessPath = paths.getJsonObject("/q/health/live");
        Assertions.assertNotNull(livenessPath);
        Set<String> livenessKeys = livenessPath.keySet();
        Assertions.assertEquals(Set.of("summary", "description", "get"), livenessKeys);

        JsonObject readinessPath = paths.getJsonObject("/q/health/ready");
        Assertions.assertNotNull(readinessPath);
        Set<String> readinessKeys = readinessPath.keySet();
        Assertions.assertEquals(Set.of("summary", "description", "get"), readinessKeys);
    }

    protected static List<String> schemaType(String responseCode, String mediaType, JsonObject responses, JsonObject schemas) {
        JsonObject responseContent = responses.getJsonObject(responseCode).getJsonObject("content");
        if (responseContent == null) {
            return null;
        }
        JsonObject schemaObj = responseContent.getJsonObject(mediaType)
                .getJsonObject("schema");

        if (schemaObj.containsKey("type")) {
            return getTypes(schemaObj);
        } else if (schemaObj.containsKey("$ref")) {
            return schemaTypeFromRef(schemaObj, schemas);
        }

        throw new IllegalArgumentException(
                "Cannot retrieve schema type for response " + responseCode + " and media type " + mediaType);
    }

    protected static List<String> schemaTypeFromRef(JsonObject responseSchema, JsonObject schemas) {
        if (responseSchema.containsKey("$ref")) {
            String schemaReference = responseSchema.getString("$ref");
            String schemaRefType = schemaReference.substring(schemaReference.lastIndexOf("/") + 1);
            return getTypes(schemas.getJsonObject(schemaRefType));
        }

        throw new IllegalArgumentException(
                "Cannot retrieve schema type for responseSchema " + responseSchema);
    }

    protected static List<String> getTypes(JsonObject schema) {
        JsonValue type = schema.get("type");

        if (type == null) {
            return null;
        } else if (type.getValueType() == ValueType.STRING) {
            return List.of(((JsonString) type).getString());
        } else if (type.getValueType() == ValueType.ARRAY) {
            return type.asJsonArray().stream().map(JsonString.class::cast).map(JsonString::getString).toList();
        }

        return null;
    }
}
