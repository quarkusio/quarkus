package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Multi;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

public class CustomSerializationOnMultiTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Item.class, UnquotedFieldNamesSerialization.class));

    @Test
    void multi() {
        when().get("/custom-serialization-multi/multi")
                .then().statusCode(200)
                .body(equalTo("[{name:\"first\",quantity:1},{name:\"second\",quantity:2}]"));
    }

    @Test
    void plain() {
        when().get("/custom-serialization-multi/plain")
                .then().statusCode(200)
                .body(equalTo("{name:\"first\",quantity:1}"));
    }

    @Path("custom-serialization-multi")
    public static class Resource {

        @GET
        @Path("multi")
        @Produces(MediaType.APPLICATION_JSON)
        @CustomSerialization(UnquotedFieldNamesSerialization.class)
        public Multi<Item> multi() {
            return Multi.createFrom().items(new Item("first", 1), new Item("second", 2));
        }

        @GET
        @Path("plain")
        @Produces(MediaType.APPLICATION_JSON)
        @CustomSerialization(UnquotedFieldNamesSerialization.class)
        public Item plain() {
            return new Item("first", 1);
        }
    }

    public static class Item {

        private final String name;
        private final int quantity;

        public Item(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static class UnquotedFieldNamesSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

        @Override
        public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
            return objectMapper.writer().without(JsonWriteFeature.QUOTE_PROPERTY_NAMES);
        }
    }
}
