package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LocalDateCustomParamConverterProviderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
