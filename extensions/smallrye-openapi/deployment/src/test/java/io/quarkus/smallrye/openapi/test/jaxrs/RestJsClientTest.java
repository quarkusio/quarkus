package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.CoreMatchers.containsString;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class RestJsClientTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GreetingResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-openapi.js-client.enabled", "true");

    @Path("/greeting")
    public static class GreetingResource {

        @GET
        public String hello() {
            return "hello";
        }

        @GET
        @Path("/{name}")
        public String helloName(@PathParam("name") String name) {
            return "hello " + name;
        }

        @POST
        public String create(String body) {
            return "created: " + body;
        }
    }

    @Test
    public void testClientLibraryIsServed() {
        RestAssured.get("/_static/quarkus-rest/rest-client.js")
                .then()
                .statusCode(200)
                .body(containsString("export class RestClient"));
    }

    @Test
    public void testTypedProxyIsServed() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.js")
                .then()
                .statusCode(200)
                .body(containsString("import { RestClient }"))
                .body(containsString("export const client"));
    }

    @Test
    public void testProxyContainsOperations() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.js")
                .then()
                .statusCode(200)
                .body(containsString("/greeting"))
                .body(containsString("client.request('GET'"))
                .body(containsString("client.request('POST'"));
    }

    @Test
    public void testProxyContainsPathParams() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.js")
                .then()
                .statusCode(200)
                .body(containsString("pathParams"))
                .body(containsString("{ name }"));
    }

    @Test
    public void testClientDeclarationsAreServed() {
        RestAssured.get("/_static/quarkus-rest/rest-client.d.ts")
                .then()
                .statusCode(200)
                .body(containsString("export class RestClient"))
                .body(containsString("export class RestError"));
    }

    @Test
    public void testProxyDeclarationsAreServed() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.d.ts")
                .then()
                .statusCode(200)
                .body(containsString("import { RestClient }"))
                .body(containsString("export declare const client: RestClient"));
    }

    @Test
    public void testProxyDeclarationsContainOperations() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.d.ts")
                .then()
                .statusCode(200)
                .body(containsString("Promise<unknown>"))
                .body(containsString("name: string | number"));
    }
}
