package io.quarkus.resteasy.reactive.server.test.simple;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LocalDateCustomParamConverterProviderTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, CustomLocalDateParamConverterProvider.class,
                            CustomLocalDateParamConverter.class));

    @Test
    public void localDateAsQueryParam() {
        RestAssured.get("/hello?date=1981-W38-6")
                .then().body(Matchers.equalTo("hello#1981-09-19"));
    }

    @Test
    public void localDateAsPathParam() {
        RestAssured.get("/hello/1995-W38-4")
                .then().body(Matchers.equalTo("hello@1995-09-21"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String helloQuery(@QueryParam("date") LocalDate date) {
            return "hello#" + date;
        }

        @GET
        @Path("{date}")
        public String helloPath(@PathParam("date") LocalDate date) {
            return "hello@" + date;
        }
    }

    public static class CustomLocalDateParamConverter implements ParamConverter<LocalDate> {

        @Override
        public LocalDate fromString(String value) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_WEEK_DATE);
        }

        @Override
        public String toString(LocalDate value) {
            return value.format(DateTimeFormatter.ISO_WEEK_DATE);
        }
    }

    @Provider
    public static class CustomLocalDateParamConverterProvider implements ParamConverterProvider {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType == LocalDate.class) {
                return (ParamConverter<T>) new CustomLocalDateParamConverter();
            }
            return null;
        }
    }
}
