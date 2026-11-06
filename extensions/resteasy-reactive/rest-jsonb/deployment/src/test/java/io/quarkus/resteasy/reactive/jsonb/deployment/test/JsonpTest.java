package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.function.Supplier;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class JsonpTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(JsonpResource.class);
                }
            });

    @Test
    public void testJsonObject() {
        RestAssured.with().contentType(ContentType.JSON).body("{\"name\": \"Bob\"}").post("/jsonp/object")
                .then().statusCode(200);
    }

    @Test
    public void testInvalidJsonObject() {
        RestAssured.with().contentType(ContentType.JSON).body("{\"name\": ").post("/jsonp/object")
                .then().statusCode(400);
    }

    @Test
    public void testInvalidJsonArray() {
        RestAssured.with().contentType(ContentType.JSON).body("[\"first\",").post("/jsonp/array")
                .then().statusCode(400);
    }

    @Test
    public void testInvalidJsonStructure() {
        RestAssured.with().contentType(ContentType.JSON).body("{\"name\": ").post("/jsonp/structure")
                .then().statusCode(400);
    }

    @Test
    public void testInvalidJsonValue() {
        RestAssured.with().contentType(ContentType.JSON).body("{\"name\": ").post("/jsonp/value")
                .then().statusCode(400);
    }

    @Path("jsonp")
    @Consumes(MediaType.APPLICATION_JSON)
    public static class JsonpResource {

        @POST
        @Path("object")
        public String object(JsonObject input) {
            return "ok";
        }

        @POST
        @Path("array")
        public String array(JsonArray input) {
            return "ok";
        }

        @POST
        @Path("structure")
        public String structure(JsonStructure input) {
            return "ok";
        }

        @POST
        @Path("value")
        public String value(JsonValue input) {
            return "ok";
        }
    }
}
