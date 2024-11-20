package io.quarkus.resteasy.reactive.server.test.converters;

import static io.restassured.RestAssured.given;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class RuntimeParamConverterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ParamConverterEndpoint.class, OptionalIntegerParamConverterProvider.class,
                                    OptionalIntegerParamConverter.class);
                }
            });

    @Test
    void sendParameters() {
        given().queryParam("number", 22)
                .when().get("/param-converter")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello, 22!"));
    }

    @Test
    void doNotSendParameters() {
        given().when().get("/param-converter")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello, world! No number was provided."));
    }

    @Test
    void sendEmptyParameter() {
        given().queryParam("number", "")
                .when().get("/param-converter")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello, world! No number was provided."));
    }

    @ApplicationScoped
    @Path("/param-converter")
    public static class ParamConverterEndpoint {

        @GET
        public Response greet(@QueryParam("number") Optional<Integer> numberOpt) {
            if (numberOpt.isPresent()) {
                return Response.ok(String.format("Hello, %s!", numberOpt.get())).build();
            } else {
                return Response.ok("Hello, world! No number was provided.").build();
            }
        }
    }

    @Provider
    @ApplicationScoped
    public static class OptionalIntegerParamConverterProvider implements ParamConverterProvider {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType.equals(Optional.class)) {
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length == 1 && typeArguments[0].equals(Integer.class)) {
                        return (ParamConverter<T>) new OptionalIntegerParamConverter();
                    }
                }
            }

            return null;
        }
    }

    public static class OptionalIntegerParamConverter implements ParamConverter<Optional<Integer>> {

        @Override
        public Optional<Integer> fromString(String value) {
            if (value == null) {
                return Optional.empty();
            }

            try {
                int parsedInt = Integer.parseInt(value);
                return Optional.of(parsedInt);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer value");
            }
        }

        @Override
        public String toString(Optional<Integer> value) {
            if (!value.isPresent()) {
                return null;
            }

            Integer intValue = value.get();
            if (intValue == null) {
                return null;
            } else {
                return intValue.toString();
            }
        }

    }

}
