package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class DeeplyNestedInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(NestedResource.class, ListPayload.class, MapPayload.class);
                }
            });

    @Test
    public void deeplyNestedListIsBadRequest() {
        RestAssured.with().contentType(ContentType.JSON).body(nestedArray(1100)).post("/nested/list")
                .then().statusCode(400);
    }

    @Test
    public void deeplyNestedMapIsBadRequest() {
        RestAssured.with().contentType(ContentType.JSON).body(nestedObject(1100)).post("/nested/map")
                .then().statusCode(400);
    }

    private static String nestedArray(int depth) {
        return "{\"items\":" + "[".repeat(depth)
                + "]".repeat(depth)
                + "}";
    }

    private static String nestedObject(int depth) {
        return "{\"data\":" + "{\"a\":".repeat(depth)
                + "null"
                + "}".repeat(depth)
                + "}";
    }

    @Path("nested")
    public static class NestedResource {

        @Path("list")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public String list(ListPayload payload) {
            return "ok";
        }

        @Path("map")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public String map(MapPayload payload) {
            return "ok";
        }
    }

    public static class ListPayload {
        public List<Object> items;
    }

    public static class MapPayload {
        public Map<String, Object> data;
    }
}
