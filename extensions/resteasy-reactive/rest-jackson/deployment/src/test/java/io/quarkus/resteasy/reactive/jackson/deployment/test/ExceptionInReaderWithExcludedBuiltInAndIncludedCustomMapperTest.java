package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.DatabindException;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExceptionInReaderWithExcludedBuiltInAndIncludedCustomMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FroMage.class, FroMageEndpoint.class, DatabindExceptionMapper.class);
                }
            }).overrideConfigKey("quarkus.class-loading.removed-resources.\"io.quarkus\\:quarkus-rest-jackson\"",
                    "io/quarkus/resteasy/reactive/jackson/runtime/mappers/BuiltinMismatchedInputExceptionMapper.class");

    @Test
    public void test() {
        RestAssured.with().contentType("application/json").body("{\"price\": \"ten\"}").put("/fromage")
                .then().statusCode(999);
    }

    @Provider
    public static class DatabindExceptionMapper implements ExceptionMapper<DatabindException> {

        @Override
        public Response toResponse(DatabindException exception) {
            return Response.status(999).build();
        }
    }
}
