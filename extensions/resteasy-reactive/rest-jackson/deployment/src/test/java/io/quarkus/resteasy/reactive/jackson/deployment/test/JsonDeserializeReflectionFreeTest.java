package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.response.ValidatableResponse;

public class JsonDeserializeReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(JsonDeserializeResource.class, CustomDeserializedDto.class,
                                    ContentUsingDto.class, ContextualUsingDto.class,
                                    UpperCaseDeserializer.class, CsvListDeserializer.class,
                                    ContextualUpperCaseDeserializer.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testCustomDeserializer() {
        echo("/json-deserialize/custom", "{\"name\":\"bob\",\"age\":42,\"tags\":\"a,b,c\"}")
                .body("name", is("BOB"))
                .body("age", is(42))
                .body("tags", contains("a", "b", "c"));
    }

    @Test
    public void testNullValue() {
        echo("/json-deserialize/custom", "{\"age\":7,\"name\":null}")
                .body("name", nullValue())
                .body("age", is(7));
    }

    @Test
    public void testContentUsing() {
        echo("/json-deserialize/content-using", "{\"tags\":[\"one\",\"two\"]}")
                .body("tags", contains("ONE", "TWO"));
    }

    @Test
    public void testContextualDeserializer() {
        echo("/json-deserialize/contextual-using", "{\"name\":\"bob\"}")
                .body("name", is("BOB"));
    }

    private static ValidatableResponse echo(String path, String request) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(200);
    }

    @Path("/json-deserialize")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonDeserializeResource {

        @POST
        @Path("/custom")
        public CustomDeserializedDto custom(CustomDeserializedDto dto) {
            return dto;
        }

        @POST
        @Path("/content-using")
        public ContentUsingDto contentUsing(ContentUsingDto dto) {
            return dto;
        }

        @POST
        @Path("/contextual-using")
        public ContextualUsingDto contextualUsing(ContextualUsingDto dto) {
            return dto;
        }
    }

    public static class CustomDeserializedDto {

        @JsonDeserialize(using = UpperCaseDeserializer.class)
        private String name;

        @JsonDeserialize(using = CsvListDeserializer.class)
        private List<String> tags;

        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class ContentUsingDto {

        @JsonDeserialize(contentUsing = UpperCaseDeserializer.class)
        private List<String> tags;

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class ContextualUsingDto {

        @JsonDeserialize(using = ContextualUpperCaseDeserializer.class)
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UpperCaseDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parser.getText().toUpperCase(Locale.ROOT);
        }
    }

    public static class CsvListDeserializer extends JsonDeserializer<List<String>> {

        @Override
        public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return List.of(parser.getText().split(","));
        }
    }

    public static class ContextualUpperCaseDeserializer extends JsonDeserializer<String>
            implements ContextualDeserializer {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parser.getText().toUpperCase(Locale.ROOT);
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
            return this;
        }
    }
}
