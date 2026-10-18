package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class PolymorphicListReflectionFreeSerializerTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(PolymorphicListResource.class,
                                    Element.class, Course.class, Quiz.class,
                                    Holder.class, PatchRequest.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testPolymorphicListInsideWrapper() {
        RestAssured
                .with()
                .body("{\"elements\":{\"value\":[{\"kind\":\"COURSE\",\"title\":\"Intro\"},{\"kind\":\"QUIZ\",\"questionCount\":5}]}}")
                .contentType("application/json")
                .post("/poly-list/wrapped")
                .then()
                .statusCode(200)
                .body("result", is("Intro,5"));
    }

    @Test
    public void testPolymorphicListDirect() {
        RestAssured
                .with()
                .body("[{\"kind\":\"COURSE\",\"title\":\"Intro\"},{\"kind\":\"QUIZ\",\"questionCount\":5}]")
                .contentType("application/json")
                .post("/poly-list/direct")
                .then()
                .statusCode(200)
                .body("result", is("Intro,5"));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Course.class, name = "COURSE"),
            @JsonSubTypes.Type(value = Quiz.class, name = "QUIZ"),
    })
    public sealed interface Element permits Course, Quiz {
        String kind();
    }

    public record Course(String kind, String title) implements Element {
    }

    public record Quiz(String kind, int questionCount) implements Element {
    }

    public static class Holder<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    public static class PatchRequest {
        private Holder<List<Element>> elements;

        public Holder<List<Element>> getElements() {
            return elements;
        }

        public void setElements(Holder<List<Element>> elements) {
            this.elements = elements;
        }
    }

    @Path("/poly-list")
    public static class PolymorphicListResource {

        @POST
        @Path("/wrapped")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public String wrapped(PatchRequest request) {
            String result = request.getElements().getValue().stream()
                    .map(PolymorphicListReflectionFreeSerializerTest::describe)
                    .collect(Collectors.joining(","));
            return "{\"result\":\"" + result + "\"}";
        }

        @POST
        @Path("/direct")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public String direct(List<Element> elements) {
            String result = elements.stream()
                    .map(PolymorphicListReflectionFreeSerializerTest::describe)
                    .collect(Collectors.joining(","));
            return "{\"result\":\"" + result + "\"}";
        }
    }

    private static String describe(Element e) {
        if (e instanceof Course c) {
            return c.title();
        } else if (e instanceof Quiz q) {
            return String.valueOf(q.questionCount());
        }
        throw new IllegalArgumentException("Unknown element type: " + e.getClass());
    }
}
