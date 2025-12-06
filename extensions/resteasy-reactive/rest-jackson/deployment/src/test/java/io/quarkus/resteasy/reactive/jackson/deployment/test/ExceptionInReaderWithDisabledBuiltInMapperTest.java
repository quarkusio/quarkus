package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExceptionInReaderWithDisabledBuiltInMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FroMage.class, FroMageEndpoint.class, CustomJsonMappingExceptionMapper.class);
                }
            }).overrideConfigKey("quarkus.rest.exception-mapping.disable-mapper-for",
                    "io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper");

    @Test
    public void test() {
        RestAssured.with().contentType("application/json").body("{\"price\": \"ten\"}").put("/fromage")
                .then().statusCode(888);
    }

    @Provider
    public static class CustomJsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

        @Override
        public Response toResponse(JsonMappingException exception) {
            return Response.status(888).entity("Custom mapper handled: " + exception.getMessage()).build();
        }
    }
}
