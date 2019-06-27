package io.quarkus.it.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * @author Ken Finnigan
 */
@QuarkusTest
public class OpenApiTestCase {

    @TestHTTPResource("openapi")
    URL uri;

    @Test
    public void testOpenAPIJSON() throws Exception {
        URLConnection connection = uri.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        JsonReader parser = Json.createReader(new ByteArrayInputStream(out.toByteArray()));
        JsonObject obj = parser.readObject();
        Assertions.assertNotNull(obj);

        Assertions.assertEquals("3.0.1", obj.getString("openapi"));
        Assertions.assertEquals("Generated API", obj.getJsonObject("info").getString("title"));
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
        String testSchemaType = schemaType("200", "*/*", testObj.getJsonObject("get").getJsonObject("responses"), schemasObj);
        String rxSchemaType = schemaType("200", "*/*", injectionObj.getJsonObject("get").getJsonObject("responses"),
                schemasObj);
        // make sure String, CompletionStage<String> and Single<String> are detected the same
        Assertions.assertEquals(testSchemaType,
                rxSchemaType,
                "Normal and RX/Single have same schema");
        JsonObject csObj = paths.getJsonObject("/test/cs");
        Assertions.assertEquals(testSchemaType,
                schemaType("200", "*/*", csObj.getJsonObject("get").getJsonObject("responses"), schemasObj),
                "Normal and RX/CS have same schema");

        JsonObject paramsObj = paths.getJsonObject("/test/params/{path}");
        JsonObject params2Obj = paths.getJsonObject("/test/params2/{path}");
        Assertions.assertEquals(paramsObj, params2Obj, "Normal and RESTEasy annotations have same schema");
    }

    protected static String schemaType(String responseCode, String mediaType, JsonObject responses, JsonObject schemas) {
        JsonObject responseContent = responses.getJsonObject(responseCode).getJsonObject("content");
        if (responseContent == null) {
            return null;
        }
        JsonObject schemaObj = responseContent.getJsonObject(mediaType)
                .getJsonObject("schema");

        if (schemaObj.containsKey("type")) {
            return schemaObj.getString("type");
        } else if (schemaObj.containsKey("$ref")) {
            String schemaReference = schemaObj.getString("$ref");
            String schemaRefType = schemaReference.substring(schemaReference.lastIndexOf("/") + 1);
            return schemas.getJsonObject(schemaRefType).getString("type");
        }

        throw new IllegalArgumentException(
                "Cannot retrieve schema type for response " + responseCode + " and media type " + mediaType);
    }
}
