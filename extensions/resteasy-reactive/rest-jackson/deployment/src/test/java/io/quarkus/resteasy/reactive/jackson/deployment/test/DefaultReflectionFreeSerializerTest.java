package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.NonBlocking;

@Disabled("This needs to be disable until we make the reflection-free serializers the default")
public class DefaultReflectionFreeSerializerTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class, TestDto.class);
                }
            });

    @Test
    public void test() {
        TestDto result = given()
                .accept("application/json")
                .get("/test")
                .then()
                .statusCode(200)
                .extract().body().as(TestDto.class);

        assertThat(result.getStackTrace()).anyMatch(s -> s.contains("$quarkusjacksonserializer.serialize"))
                .noneMatch(s -> s.contains("BeanSerializer.serialize"));
    }

    @Path("/test")
    @NonBlocking
    @DisableSecureSerialization
    public static class TestResource {

        @GET
        public TestDto get() {
            return new TestDto();
        }
    }

    public static class TestDto {

        private final AtomicReference<List<String>> stacktrace = new AtomicReference<>();

        @JsonProperty
        public List<String> getStackTrace() {
            return stacktrace.updateAndGet(current -> current != null ? current : computeStackTrace());
        }

        @JsonProperty
        public void setStackTrace(List<String> stacktrace) {
            this.stacktrace.set(stacktrace);
        }

        private List<String> computeStackTrace() {
            return StackWalker.getInstance()
                    .walk(frames -> frames.limit(10)
                            .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                            .collect(Collectors.toList()));
        }
    }

}
