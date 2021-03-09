package io.quarkus.tck.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.testng.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.rest.client.tck.WiremockArquillianTest;
import org.eclipse.microprofile.rest.client.tck.interfaces.JsonPClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

/*
copied from https://github.com/eclipse/microprofile-rest-client/blob/1.4.X-service/tck/src/main/java/org/eclipse/microprofile/rest/client/tck/InvokeWithJsonPProviderTest.java
 */
public class CustomInvokeWithJsonPProviderTest extends WiremockArquillianTest {

    private static final String CDI = "cdi";
    private static final String BUILT = "built";

    @Deployment
    public static WebArchive createDeployment() {
        StringAsset mpConfig = new StringAsset(JsonPClient.class.getName() + "/mp-rest/url=" + getStringURL());
        return ShrinkWrap.create(WebArchive.class, CustomInvokeWithJsonPProviderTest.class.getSimpleName() + ".war")
                .addClasses(JsonPClient.class, WiremockArquillianTest.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(mpConfig, "classes/META-INF/microprofile-config.properties");
    }

    @Inject
    @RestClient
    private JsonPClient cdiJsonPClient;

    private JsonPClient builtJsonPClient;

    public void setupClient() {
        builtJsonPClient = RestClientBuilder.newBuilder()
                .baseUri(getServerURI())
                .build(JsonPClient.class);
    }

    @Test
    public void testGetExecutesForBothClients() {
        setupClient();
        testGet(builtJsonPClient, BUILT);
        testGet(cdiJsonPClient, CDI);
    }

    @Test
    public void testGetSingleExecutesForBothClients() {
        setupClient();
        testGetSingle(builtJsonPClient, BUILT);
        testGetSingle(cdiJsonPClient, CDI);
    }

    @Test
    public void testPostExecutes() {
        setupClient();
        testPost(builtJsonPClient, BUILT);
        testPost(cdiJsonPClient, CDI);
    }

    @Test
    public void testPutExecutes() {
        setupClient();
        testPut(builtJsonPClient, BUILT);
        testPut(cdiJsonPClient, CDI);
    }

    private void testGet(JsonPClient client, String clientType) {
        reset();
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"key\": \"value\"}, {\"key\": \"anotherValue\"}]")));
        JsonArray jsonArray = client.get();
        assertEquals(jsonArray.size(), 2, "Expected 2 values in the array for client " + clientType);
        List<JsonObject> jsonObjects = jsonArray.getValuesAs(JsonObject.class);
        JsonObject one = jsonObjects.get(0);
        assertEquals(one.keySet().size(), 1, "There should only be one key in object 1 for client " + clientType);
        assertEquals(one.getString("key"), "value", "The value of 'key' on object 1 should be 'value' in client " + clientType);

        JsonObject two = jsonObjects.get(1);
        assertEquals(two.keySet().size(), 1, "There should only be one key in object 2 for client " + clientType);
        assertEquals(two.getString("key"), "anotherValue",
                "The value of 'key' on object 2 should be 'anotherValue' in client " + clientType);

    }

    private void testGetSingle(JsonPClient client, String clientType) {
        reset();
        stubFor(get(urlEqualTo("/id"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"key\": \"value\"}")));
        JsonObject jsonObject = client.get("id");
        assertEquals(jsonObject.keySet().size(), 1, "There should only be one key in object for client " + clientType);
        assertEquals(jsonObject.getString("key"), "value",
                "The value of 'key' on object should be 'value' in client " + clientType);

    }

    private void testPost(JsonPClient client, String clientType) {
        reset();
        stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));

        JsonObject jsonObject = Json.createObjectBuilder().add("someKey", "newValue").build();
        String jsonObjectAsString = jsonObject.toString();
        Response response = client.post(jsonObject);
        response.close();
        assertEquals(response.getStatus(), 200, "Expected a 200 OK on client " + clientType);

        verify(1, postRequestedFor(urlEqualTo("/")).withRequestBody(equalTo(jsonObjectAsString)));
    }

    private void testPut(JsonPClient client, String clientType) {
        reset();
        stubFor(put(urlEqualTo("/id"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200).withBody("{\"someOtherKey\":\"newValue\"}")));

        JsonObject jsonObject = Json.createObjectBuilder().add("someKey", "newValue").build();
        String jsonObjectAsString = jsonObject.toString();
        JsonObject response = client.update("id", jsonObject);
        assertEquals(response.getString("someOtherKey"), "newValue",
                "The value of 'someOtherKey' on response should be 'someOtherKey' in client " + clientType);

        verify(1, putRequestedFor(urlEqualTo("/id")).withRequestBody(equalTo(jsonObjectAsString)));
    }
}
