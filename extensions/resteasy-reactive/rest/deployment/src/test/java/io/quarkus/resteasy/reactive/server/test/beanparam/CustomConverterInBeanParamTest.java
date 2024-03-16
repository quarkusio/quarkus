package io.quarkus.resteasy.reactive.server.test.beanparam;

import static org.hamcrest.CoreMatchers.equalTo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomConverterInBeanParamTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SearchResource.class, FilterData.class,
                            JavaTimeParamConverterProvider.class, LocalDateTimeParamConverter.class));

    @Test
    void shouldCustomConvertBeUsedForLocalDateTimeInFilterData() {
        LocalDateTime since = LocalDateTime.now();
        String request = since.format(DateTimeFormatter.ISO_DATE_TIME);
        String expected = since.plusYears(1).format(DateTimeFormatter.ISO_DATE_TIME);

        RestAssured.get("/search?since=" + request)
                .then()
                .statusCode(200)
                .body(equalTo("Got: " + expected));
    }

    @Path("/search")
    public static class SearchResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String search(@BeanParam FilterData filter) {
            return "Got: " + filter.getSince().plusYears(1).format(DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    public static class FilterData {

        @QueryParam("since")
        private LocalDateTime since;

        public LocalDateTime getSince() {
            return since;
        }

        public void setSince(LocalDateTime since) {
            this.since = since;
        }
    }

    public static class LocalDateTimeParamConverter implements ParamConverter<LocalDateTime> {

        @Override
        public LocalDateTime fromString(String value) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        }

        @Override
        public String toString(LocalDateTime value) {
            return value.format(DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    @Provider
    public static class JavaTimeParamConverterProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType == LocalDateTime.class) {
                return (ParamConverter<T>) new LocalDateTimeParamConverter();
            }

            return null;
        }

    }
}
