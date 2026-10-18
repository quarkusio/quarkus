package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/55255
 *
 * With reflection-free serializers enabled the generated deserialiser throws a plain
 * {@code JsonMappingException} for unknown fields instead of {@code UnrecognizedPropertyException}
 * (a subtype of {@code MismatchedInputException}), bypassing custom exception mappers that target
 * {@code MismatchedInputException}.
 */
public class UnknownFieldCustomExceptionMapperTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GreetingRequest.class, GreetingEndpoint.class,
                                    CustomMismatchedInputMapper.class);
                }
            })
            .overrideConfigKey("quarkus.jackson.fail-on-unknown-properties", "true")
            .overrideConfigKey("quarkus.rest.jackson.optimization.enable-reflection-free-serializers", "true");

    @Test
    void unknownFieldShouldBeHandledByCustomMismatchedInputMapper() {
        given().contentType("application/json")
                .body("{\"name\": \"world\", \"evil\": \"data\"}")
                .when().post("/greeting")
                .then()
                .statusCode(422)
                .body("handledBy", is("MismatchedInputExceptionMapper"));
    }

    public record GreetingRequest(@JsonProperty("name") String name) {
    }

    @Path("/greeting")
    public static class GreetingEndpoint {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public String greeting(GreetingRequest request) {
            return "{\"message\":\"Hello " + request.name() + "\"}";
        }
    }

    @Provider
    public static class CustomMismatchedInputMapper implements ExceptionMapper<MismatchedInputException> {
        @Override
        public Response toResponse(MismatchedInputException exception) {
            return Response.status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"handledBy\":\"MismatchedInputExceptionMapper\"}")
                    .build();
        }
    }
}
