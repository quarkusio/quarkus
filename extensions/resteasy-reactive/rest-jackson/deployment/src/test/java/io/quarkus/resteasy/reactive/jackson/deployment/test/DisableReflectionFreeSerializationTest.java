package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.resteasy.reactive.jackson.DisableReflectionFreeSerialization;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@link DisableReflectionFreeSerialization} excludes a single class from the reflection-free
 * optimization, while the other classes of the same application keep using their generated serializers and
 * deserializers.
 * <p>
 * Each DTO captures the stack trace of the thread that is (de)serializing it, which tells the two code paths apart: the
 * generated classes are named after the DTO plus a {@code $quarkusjackson(de)serializer} suffix, whereas Jackson's
 * reflection-based path goes through its own {@code Bean(De)Serializer}.
 * <p>
 * The deserialization trace is captured in the setter, so both code paths must populate the DTOs through it. That is why
 * the DTOs below only declare a no-argument constructor: these classes are compiled with {@code -parameters} and
 * Jackson 3 detects constructor parameter names on its own, so adding a {@code (String name)} constructor would make it
 * an implicit properties-based creator. Jackson would then populate the field through that constructor and never call
 * the setter, leaving the trace empty.
 */
public class DisableReflectionFreeSerializationTest {

    private static final String GENERATED_SERIALIZER = "$quarkusjacksonserializer";
    private static final String GENERATED_DESERIALIZER = "$quarkusjacksondeserializer";
    private static final String REFLECTION_BASED_SERIALIZER = "BeanSerializer";
    private static final String REFLECTION_BASED_DESERIALIZER = "BeanDeserializer";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class, OptimizedDto.class, ExcludedDto.class)
                    .addAsResource(new StringAsset(
                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                            "application.properties"));

    @Test
    public void testOptimizedClassIsSerializedByGeneratedSerializer() {
        assertThat(serializationStackTraceOf("/test/optimized"))
                .anyMatch(frame -> frame.contains(GENERATED_SERIALIZER))
                .noneMatch(frame -> frame.contains(REFLECTION_BASED_SERIALIZER));
    }

    @Test
    public void testExcludedClassIsSerializedByJackson() {
        assertThat(serializationStackTraceOf("/test/excluded"))
                .anyMatch(frame -> frame.contains(REFLECTION_BASED_SERIALIZER))
                .noneMatch(frame -> frame.contains(GENERATED_SERIALIZER));
    }

    @Test
    public void testOptimizedClassIsDeserializedByGeneratedDeserializer() {
        assertThat(deserializationStackTraceOf("/test/optimized"))
                .anyMatch(frame -> frame.contains(GENERATED_DESERIALIZER))
                .noneMatch(frame -> frame.contains(REFLECTION_BASED_DESERIALIZER));
    }

    @Test
    public void testExcludedClassIsDeserializedByJackson() {
        assertThat(deserializationStackTraceOf("/test/excluded"))
                .anyMatch(frame -> frame.contains(REFLECTION_BASED_DESERIALIZER))
                .noneMatch(frame -> frame.contains(GENERATED_DESERIALIZER));
    }

    @Test
    public void testExcludedClassIsStillProcessedCorrectly() {
        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/excluded")
                .then()
                .statusCode(200)
                .body("name", Matchers.is("excluded"));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"hello\"}")
                .post("/test/excluded/name")
                .then()
                .statusCode(200)
                .body(Matchers.is("hello"));
    }

    private static List<String> serializationStackTraceOf(String path) {
        return given()
                .accept(MediaType.APPLICATION_JSON)
                .get(path)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getList("stackTrace", String.class);
    }

    private static List<String> deserializationStackTraceOf(String path) {
        String[] frames = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"hello\"}")
                .post(path)
                .then()
                .statusCode(200)
                .extract().body().as(String[].class);
        return Arrays.asList(frames);
    }

    private static List<String> captureStackTrace() {
        return StackWalker.getInstance()
                .walk(frames -> frames.limit(30)
                        .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                        .collect(Collectors.toList()));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/optimized")
        @Produces(MediaType.APPLICATION_JSON)
        public OptimizedDto getOptimized() {
            return OptimizedDto.withName("optimized");
        }

        @GET
        @Path("/excluded")
        @Produces(MediaType.APPLICATION_JSON)
        public ExcludedDto getExcluded() {
            return ExcludedDto.withName("excluded");
        }

        @POST
        @Path("/optimized")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public List<String> postOptimized(OptimizedDto dto) {
            return dto.deserializationTrace();
        }

        @POST
        @Path("/excluded")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public List<String> postExcluded(ExcludedDto dto) {
            return dto.deserializationTrace();
        }

        @POST
        @Path("/excluded/name")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.TEXT_PLAIN)
        public String postExcludedName(ExcludedDto dto) {
            return dto.getName();
        }
    }

    public static class OptimizedDto {

        private final AtomicReference<List<String>> stacktrace = new AtomicReference<>();
        private List<String> deserializationTrace;
        private String name;

        public OptimizedDto() {
        }

        /**
         * Populates the DTO without going through {@link #setName(String)}, so that the serialization tests never see a
         * deserialization trace left behind by the resource method itself.
         */
        static OptimizedDto withName(String name) {
            OptimizedDto dto = new OptimizedDto();
            dto.name = name;
            return dto;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            this.deserializationTrace = captureStackTrace();
        }

        @JsonProperty
        public List<String> getStackTrace() {
            return stacktrace.updateAndGet(current -> current != null ? current : captureStackTrace());
        }

        List<String> deserializationTrace() {
            return deserializationTrace;
        }
    }

    @DisableReflectionFreeSerialization
    public static class ExcludedDto {

        private final AtomicReference<List<String>> stacktrace = new AtomicReference<>();
        private List<String> deserializationTrace;
        private String name;

        public ExcludedDto() {
        }

        /**
         * Populates the DTO without going through {@link #setName(String)}, so that the serialization tests never see a
         * deserialization trace left behind by the resource method itself.
         */
        static ExcludedDto withName(String name) {
            ExcludedDto dto = new ExcludedDto();
            dto.name = name;
            return dto;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            this.deserializationTrace = captureStackTrace();
        }

        @JsonProperty
        public List<String> getStackTrace() {
            return stacktrace.updateAndGet(current -> current != null ? current : captureStackTrace());
        }

        List<String> deserializationTrace() {
            return deserializationTrace;
        }
    }
}
