package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExceptionInReaderWithCustomMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FroMage.class, FroMageEndpoint.class, CustomMismatchedInputExceptionMapper.class);
                }
            });

    @Test
    public void test() {
        RestAssured.with().contentType("application/json").body("{\"name\": \"brie\"}").put("/fromage")
                .then().statusCode(406);
    }

    @Provider
    public static class CustomMismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {
        @Override
        public Response toResponse(MismatchedInputException exception) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }

}
