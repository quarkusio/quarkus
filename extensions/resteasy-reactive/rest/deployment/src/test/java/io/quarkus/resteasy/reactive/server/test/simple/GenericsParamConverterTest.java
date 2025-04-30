package io.quarkus.resteasy.reactive.server.test.simple;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GenericsParamConverterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestEnum.class, Wrapper.class,
                            WrapperParamConverterProvider.class, WrapperParamConverterProvider.WrapperParamConverter.class,
                            TestResource.class));

    @Test
    public void wrapper() {
        given()
                .when().get("/test/single?wrapper=ACTIVE")
                .then()
                .statusCode(200)
                .body(is("ACTIVE"));
    }

    @Test
    public void wrapperList() {
        given()
                .when().get("/test/list?wrapperList=INACTIVE&wrapperList=ACTIVE")
                .then()
                .statusCode(200)
                .body(is("INACTIVE,ACTIVE"));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/list")
        public String list(@QueryParam("wrapperList") final List<Wrapper<TestEnum>> wrapperList) {
            return wrapperList.stream().map(w -> w.getValue().name()).collect(Collectors.joining(","));
        }

        @GET
        @Path("/single")
        public String single(@QueryParam("wrapper") final Wrapper<TestEnum> wrapper) {
            return wrapper.getValue().toString();
        }
    }

    public enum TestEnum {
        ACTIVE,
        INACTIVE
    }

    public static class Wrapper<E extends Enum<E>> {
        private final E value;

        public Wrapper(final E value) {
            this.value = value;
        }

        public E getValue() {
            return value;
        }
    }

    @Provider
    public static class WrapperParamConverterProvider implements ParamConverterProvider {

        @Override
        @SuppressWarnings("unchecked")
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType,
                final Annotation[] annotations) {
            if (Wrapper.class.isAssignableFrom(rawType)) {
                return (ParamConverter<T>) new WrapperParamConverter();
            }
            return null;
        }

        public static class WrapperParamConverter implements ParamConverter<Wrapper<?>> {

            @Override
            public Wrapper<?> fromString(String value) {
                return new Wrapper<>(Enum.valueOf(TestEnum.class, value));
            }

            @Override
            public String toString(Wrapper<?> wrapper) {
                return wrapper != null ? wrapper.getValue().toString() : null;
            }
        }
    }
}
