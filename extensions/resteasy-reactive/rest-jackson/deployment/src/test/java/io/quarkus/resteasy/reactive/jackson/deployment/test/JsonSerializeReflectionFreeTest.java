package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import io.quarkus.test.QuarkusExtensionTest;

public class JsonSerializeReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(JsonSerializeResource.class, CustomSerializedDto.class,
                                    ContentUsingDto.class, ContextualUsingDto.class,
                                    UpperCaseSerializer.class, MissingValueSerializer.class,
                                    ContextualUpperCaseSerializer.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testCustomSerializer() {
        CustomSerializedDto result = get("/json-serialize/custom", CustomSerializedDto.class);

        assertThat(result.getName()).isEqualTo("BOB");
        assertThat(result.getNickName()).isEqualTo("<none>");
        assertThat(result.getAge()).isEqualTo(42);
        assertThat(result.getSerializationStack())
                .anyMatch(frame -> frame.contains("$quarkusjacksonserializer.serialize"))
                .noneMatch(frame -> frame.contains("BeanSerializer.serialize"));
    }

    @Test
    public void testContentUsing() {
        ContentUsingDto result = get("/json-serialize/content-using", ContentUsingDto.class);

        assertThat(result.getTags()).containsExactly("ONE", "TWO");
        assertThat(result.getSerializationStack())
                .anyMatch(frame -> frame.contains("BeanSerializer.serialize"))
                .noneMatch(frame -> frame.contains("$quarkusjacksonserializer.serialize"));
    }

    @Test
    public void testContextualSerializer() {
        ContextualUsingDto result = get("/json-serialize/contextual-using", ContextualUsingDto.class);

        assertThat(result.getName()).isEqualTo("BOB");
        assertThat(result.getSerializationStack())
                .anyMatch(frame -> frame.contains("BeanSerializer.serialize"))
                .noneMatch(frame -> frame.contains("$quarkusjacksonserializer.serialize"));
    }

    private static <T> T get(String path, Class<T> type) {
        return given()
                .accept(MediaType.APPLICATION_JSON)
                .get(path)
                .then()
                .statusCode(200)
                .extract().body().as(type);
    }

    @Path("/json-serialize")
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonSerializeResource {

        @GET
        @Path("/custom")
        public CustomSerializedDto custom() {
            return new CustomSerializedDto("bob", null, 42);
        }

        @GET
        @Path("/content-using")
        public ContentUsingDto contentUsing() {
            return new ContentUsingDto(List.of("one", "two"));
        }

        @GET
        @Path("/contextual-using")
        public ContextualUsingDto contextualUsing() {
            return new ContextualUsingDto("bob");
        }
    }

    /**
     * Captures the stack that serialized the object, to tell whether the generated serializer or Jackson's own
     * BeanSerializer produced the json.
     */
    public abstract static class StackCapturingDto {

        private List<String> serializationStack;

        public List<String> getSerializationStack() {
            if (serializationStack == null) {
                serializationStack = StackWalker.getInstance()
                        .walk(frames -> frames.limit(10)
                                .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                                .collect(Collectors.toList()));
            }
            return serializationStack;
        }

        public void setSerializationStack(List<String> serializationStack) {
            this.serializationStack = serializationStack;
        }
    }

    public static class CustomSerializedDto extends StackCapturingDto {

        @JsonSerialize(using = UpperCaseSerializer.class)
        private String name;

        @JsonSerialize(using = UpperCaseSerializer.class, nullsUsing = MissingValueSerializer.class)
        private String nickName;

        private int age;

        public CustomSerializedDto() {
        }

        public CustomSerializedDto(String name, String nickName, int age) {
            this.name = name;
            this.nickName = nickName;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class ContentUsingDto extends StackCapturingDto {

        @JsonSerialize(contentUsing = UpperCaseSerializer.class)
        private List<String> tags;

        public ContentUsingDto() {
        }

        public ContentUsingDto(List<String> tags) {
            this.tags = tags;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class ContextualUsingDto extends StackCapturingDto {

        @JsonSerialize(using = ContextualUpperCaseSerializer.class)
        private String name;

        public ContextualUsingDto() {
        }

        public ContextualUsingDto(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UpperCaseSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator generator, SerializerProvider serializerProvider)
                throws IOException {
            generator.writeString(value.toUpperCase(Locale.ROOT));
        }
    }

    public static class MissingValueSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator generator, SerializerProvider serializerProvider)
                throws IOException {
            generator.writeString("<none>");
        }
    }

    public static class ContextualUpperCaseSerializer extends JsonSerializer<String> implements ContextualSerializer {

        @Override
        public void serialize(String value, JsonGenerator generator, SerializerProvider serializerProvider)
                throws IOException {
            generator.writeString(value.toUpperCase(Locale.ROOT));
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider serializerProvider, BeanProperty property) {
            return this;
        }
    }
}
